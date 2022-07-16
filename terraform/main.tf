terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.26"
    }

    databricks = {
      source = "databricks/databricks"
      version = ">= 1.0.1"
    }
  }
}

provider "azurerm" {
  features {}
}

provider "databricks" {
  host = var.databricks_workspace_url
}

variable "databricks_workspace_url" {
  description = "The URL to the Azure Databricks workspace (must start with https://)"
  type = string
//  default = "https://adb-8308927019810375.15.azuredatabricks.net/?o=8308927019810375#"
}

variable "resource_prefix" {
  description = "The prefix to use when naming the notebook and job"
  type = string
  default = "variant-spark-examples"
}

variable "email_notifier" {
  description = "The email address to send job status to"
  type = list(string)
  default = ["dlevy@example.com"]
}

// Get information about the Databricks user that is calling
// the Databricks API (the one associated with "databricks_connection_profile").
data "databricks_current_user" "me" {}

// Create a simple, sample notebook. Store it in a subfolder within
// the Databricks current user's folder. The notebook contains the
// following basic Spark code in Python.
resource "databricks_notebook" "this" {
  path     = "${data.databricks_current_user.me.home}/Terraform/${var.resource_prefix}-notebook.ipynb"
  language = "PYTHON"
  content_base64 = base64encode(<<-EOT
    # created from ${abspath(path.module)}
    display(spark.range(10))
    EOT
  )
}

// Create a job to run the sample notebook. The job will create
// a cluster to run on. The cluster will use the smallest available
// node type and run the latest version of Spark.

// Get the smallest available node type to use for the cluster. Choose
// only from among available node types with local storage.
data "databricks_node_type" "smallest" {
  local_disk = true
}

// Get the latest Spark version to use for the cluster.
data "databricks_spark_version" "latest" {}

// Create the job, emailing notifiers about job success or failure.
resource "databricks_job" "this" {
  name = "${var.resource_prefix}-job-${data.databricks_current_user.me.alphanumeric}"
  
  // Example of a git sourced job
  git_source {
    url = "https://github.com/davidglevy/variant-spark-examples"
    branch = "main"
    provider = "github"
  }



  job_cluster {
    job_cluster_key = "genomics_job_cluster"
    new_cluster {
      num_workers   = 3
      spark_version = data.databricks_spark_version.latest.id
      node_type_id  = data.databricks_node_type.smallest.id

    }
  }

  task {
    task_key = "Ingest"
    job_cluster_key = "genomics_job_cluster"
    notebook_task {
      notebook_path = "jobs/01 Jobs Example - Ingest"
    }
    library {
      maven {
        coordinates = "au.csiro.aehrc.variant-spark:variant-spark_2.12:0.5.0"
      }
    }
  }
  
  task {
    task_key = "Setup_Database"
    job_cluster_key = "genomics_job_cluster"
    notebook_task {
      notebook_path = "jobs/01 Jobs Example - Setup Databases"
    }
    library {
      maven {
        coordinates = "au.csiro.aehrc.variant-spark:variant-spark_2.12:0.5.0"
      }
    }
  }
 
 task {
    task_key = "Process"
    job_cluster_key = "genomics_job_cluster"
    notebook_task {
      notebook_path = "jobs/02 Jobs Example - Process"
    }
    depends_on {
      task_key = "Ingest"
    }
    depends_on {
      task_key = "Setup_Database"
    }
    library {
      maven {
        coordinates = "au.csiro.aehrc.variant-spark:variant-spark_2.12:0.5.0"
      }
    }
  }

  task {
    task_key = "Visualise"
    job_cluster_key = "genomics_job_cluster"
    notebook_task {
      notebook_path = "jobs/03 Jobs Example - Visualise"
    }
    depends_on {
      task_key = "Process"
    }
    library {
      maven {
        coordinates = "au.csiro.aehrc.variant-spark:variant-spark_2.12:0.5.0"
      }
    }
  }

}


//  new_cluster {
//    num_workers   = 3
//    spark_version = data.databricks_spark_version.latest.id
//    node_type_id  = data.databricks_node_type.smallest.id
//  }
//  notebook_task {
//    notebook_path = databricks_notebook.this.path
//  }
//  email_notifications {
//    on_success = var.email_notifier
//    on_failure = var.email_notifier
//  }
//}

// Print the URL to the notebook.
//output "notebook_url" {
//  value = databricks_notebook.this.url
//}

// Print the URL to the job.
//output "job_url" {
//  value = databricks_job.this.url
//}

resource "databricks_repo" "examples" {
  url = "https://github.com/davidglevy/variant-spark-examples"
}

resource "databricks_repo" "genomics_notebook" {
  url = "https://github.com/microsoft/genomicsnotebook"
}


