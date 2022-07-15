// Databricks notebook source
// MAGIC %md
// MAGIC ###2. LOAD VARIANTS using VariantSpark     
// MAGIC 
// MAGIC 1. Use Scala to import the VSContext and ImportanceAnalysis objects from the VariantSpark library  
// MAGIC 2. Create an instance of the VSContext object, passing in an instance of the Spark Context object to it  
// MAGIC 3. Call the featureSource method on the instance of the vsContext object and pass in the path the the demo feature file  
// MAGIC      to load the variants from the vcf file
// MAGIC 4. Display the first 10 sample names

// COMMAND ----------

import au.csiro.variantspark.api.VSContext
import au.csiro.variantspark.api.ImportanceAnalysis
implicit val vsContext = VSContext(spark)

val featureSource = vsContext.featureSource("/vs-datasets/hipsterIndex/hipster.vcf.gz")
println("Names of loaded samples:")
println(featureSource.sampleNames.take(10))

// COMMAND ----------

// MAGIC %md
// MAGIC ###3. LOAD LABELS using VariantSpark   
// MAGIC 
// MAGIC 1. Use Scala to call the labelSource method on the instance of the vsContext object and pass in the path the the demo label file  
// MAGIC 2. Display the first 10 phenotype labels

// COMMAND ----------

val labelSource  = vsContext.labelSource("/vs-datasets/hipsterIndex/hipster_labels.txt", "label")
println("First few labels:")
println(labelSource.getLabels(featureSource.sampleNames).toList.take(10))

// COMMAND ----------

// MAGIC %md
// MAGIC ###4. CONFIGURE ANALYSIS using VariantSpark   
// MAGIC 
// MAGIC 1. Use Scala to create an instance of the ImportanceAnalysis object
// MAGIC 2. Pass the featureSoure (feature file), labelSource (label file), and number of trees to the instance

// COMMAND ----------

val importanceAnalysis = ImportanceAnalysis(featureSource, labelSource, nTrees = 1000)

// COMMAND ----------

// MAGIC %md
// MAGIC ###5. RUN ANALYSIS using VariantSpark   
// MAGIC 
// MAGIC Unlike other statistical approaches, random forests have the advantage of not needing the data to be extensively pre-processed, so the analysis can be triggered on the loaded data directly. The analysis will take around 4 minutes on a Databricks community (one node) cluster.  
// MAGIC 
// MAGIC 1. Use Scala to call the `variableImportance` method on the instance of the `ImportanceAnalysis` object to calcuate the variant importance attributing to the phenotype
// MAGIC 2. Cache the analysis results into a SparkSQL table  
// MAGIC 3. Display the tabular results    

// COMMAND ----------

val variableImportance = importanceAnalysis.variableImportance
variableImportance.cache().registerTempTable("importance")
display(variableImportance)
