# Databricks notebook source
dbutils.widgets.text("database_name", "variant_spark", "Database Name")

# COMMAND ----------

database_name = dbutils.widgets.get("database_name")

# COMMAND ----------

from pyspark.sql.functions import col, lit

db_exists = spark.sql("""
  SHOW DATABASES
""").filter(col("databaseName") == lit("variant_spark")).count() == 1
display(db_exists)

# COMMAND ----------

def createDatabase(to_create):
    result = spark.sql(f"""
        CREATE DATABASE {to_create}
    """)
    print(f"Created database [{to_create}]")
    display(result)

# COMMAND ----------

if db_exists:
    print(f"Database [{database_name}] already exists")
else:
    print(f"Database [{database_name}] does not exist, creating now")
    createDatabase(database_name)
    


# COMMAND ----------


