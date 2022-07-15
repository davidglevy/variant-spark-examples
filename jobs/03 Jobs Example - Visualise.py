# Databricks notebook source
# MAGIC %md
# MAGIC ###6a. VISUALIZE ANALYSIS using SparkSQL
# MAGIC 1. Query the SparkSQL table to display the top 25 results in descending order 
# MAGIC 2. Plot the results into a bar chart using the visualization feature in Databricks
# MAGIC 
# MAGIC *Note: the Hipster-Index is constructed from 4 SNPs so we expect the importance to be limited to these SNPs and the ones on [linkage disequilibrium (LD)](https://en.wikipedia.org/wiki/Linkage_disequilibrium) with them.* 

# COMMAND ----------

dbutils.widgets.text("database_name", "variant_spark", "Database Name")

# COMMAND ----------

database_name = dbutils.widgets.get("database_name")

# COMMAND ----------

# MAGIC %sql
# MAGIC SELECT * FROM `$database_name`.variable_importance ORDER BY importance DESC LIMIT 25

# COMMAND ----------

# MAGIC %md
# MAGIC ###6b. VISUALIZE ANALYSIS using Python  
# MAGIC 
# MAGIC 1. Query the SparkSQL table to display the top 25 results in descending order 
# MAGIC 2. Plot the results into a line chart using the visualization feature in the python libraries

# COMMAND ----------

import matplotlib.pyplot as plt

database_name = dbutils.widgets.get("database_name")

importance = sqlContext.sql(f"SELECT * FROM {database_name}.variable_importance ORDER BY importance DESC LIMIT 25")
display(importance)

# COMMAND ----------


importanceDF = importance.toPandas()
ax = importanceDF.plot(x="variable", y="importance",lw=3,colormap='Reds_r',title='Importance in Descending Order', fontsize=9)
ax.set_xlabel("variable")
ax.set_ylabel("importance")
plt.xticks(rotation=12)
plt.grid(True)
plt.show()
display()

# COMMAND ----------

# MAGIC %md
# MAGIC ###7. LOAD ANALYSIS into an R session 
# MAGIC 
# MAGIC 1. Using the R collect method to load the SparkSQL table into a local R session  
# MAGIC 2. List the results using the head method in R  

# COMMAND ----------

# MAGIC %r
# MAGIC library(SparkR)
# MAGIC 
# MAGIC database_name <- dbutils.widgets.get("database_name")
# MAGIC print(database_name)
# MAGIC 
# MAGIC stmt <- paste('SELECT * FROM ',database_name, '.variable_importance ORDER BY importance DESC', sep="")
# MAGIC print(stmt)
# MAGIC 
# MAGIC importance_df_full  = collect(sql(stmt))
# MAGIC head(importance_df_full)

# COMMAND ----------

# MAGIC %md
# MAGIC ###8. VISUALIZE ANALYSIS using R   
# MAGIC 
# MAGIC 1. Load the ggplot2 library   
# MAGIC 2. Use the R collect method to load the SparkSQL table into a local R session  
# MAGIC 3. Plot the results on a bar chart

# COMMAND ----------

# MAGIC %r
# MAGIC 
# MAGIC 
# MAGIC stmt <- paste('SELECT * FROM ', database_name, '.variable_importance ORDER BY variable DESC limit 20', sep="")
# MAGIC print(stmt)
# MAGIC 
# MAGIC library(ggplot2)
# MAGIC importance_df  = collect(sql(stmt))
# MAGIC ggplot(importance_df, aes(x=variable, y=importance)) + geom_bar(stat='identity') + scale_x_discrete(limits=importance_df[order(importance_df$importance), "variable"]) + coord_flip()

# COMMAND ----------

# MAGIC %md
# MAGIC ##Results interpretation
# MAGIC The plot above shows that VariantSpark has recovered the correct genotypes of this multivariate phenotype with interacting features (multiplicative and additive effects). 
# MAGIC 
# MAGIC ![Hipster-Index](https://s3.us-east-2.amazonaws.com/csiro-graphics/HipsterSignatureGraphic-new.png)
# MAGIC 
# MAGIC 1. __chr2_223034082__ (rs2218065) encoding for monobrow is the most important feature 
# MAGIC 2. a group of SNPs encoding for the MEGF10 gene (__chr5_126626044__), which is involved in Retina horizontal cell formation   
# MAGIC    as the second most important marker, explaining why hipsters prefer checked shirts
# MAGIC 3. __chr7_17284577__ (rs4410790) the marker for increased coffee consuption is ranked third  
# MAGIC 4. __chr4_54511913__ (rs4864809) the marker for beards is fourth
# MAGIC 
# MAGIC The last two are in swapped order compared to the formular of the HipsterIndex, however with 0.5 and 1 as weight they may be difficult to differentiate. 

# COMMAND ----------

# MAGIC %md
# MAGIC ###Credit
# MAGIC 
# MAGIC ![VariantSpark](https://s3.us-east-2.amazonaws.com/csiro-graphics/variant-spark.png)  
# MAGIC Transformational Bioinformatics team has developed VariantSpark and put together this illustrative example. Thank you to Lynn Langit for input on the presentation of this notebook. 
