// Databricks notebook source
// MAGIC %md
// MAGIC ###6a. VISUALIZE ANALYSIS using SparkSQL
// MAGIC 1. Query the SparkSQL table to display the top 25 results in descending order 
// MAGIC 2. Plot the results into a bar chart using the visualization feature in Databricks
// MAGIC 
// MAGIC *Note: the Hipster-Index is constructed from 4 SNPs so we expect the importance to be limited to these SNPs and the ones on [linkage disequilibrium (LD)](https://en.wikipedia.org/wiki/Linkage_disequilibrium) with them.* 

// COMMAND ----------

// MAGIC %sql
// MAGIC SELECT * FROM importance ORDER BY importance DESC LIMIT 25

// COMMAND ----------

// MAGIC %md
// MAGIC ###6b. VISUALIZE ANALYSIS using Python  
// MAGIC 
// MAGIC 1. Query the SparkSQL table to display the top 25 results in descending order 
// MAGIC 2. Plot the results into a line chart using the visualization feature in the python libraries

// COMMAND ----------

// MAGIC %python
// MAGIC import matplotlib.pyplot as plt
// MAGIC importance = sqlContext.sql("SELECT * FROM importance ORDER BY importance DESC LIMIT 25")
// MAGIC importanceDF = importance.toPandas()
// MAGIC ax = importanceDF.plot(x="variable", y="importance",lw=3,colormap='Reds_r',title='Importance in Descending Order', fontsize=9)
// MAGIC ax.set_xlabel("variable")
// MAGIC ax.set_ylabel("importance")
// MAGIC plt.xticks(rotation=12)
// MAGIC plt.grid(True)
// MAGIC plt.show()
// MAGIC display()

// COMMAND ----------

// MAGIC %md
// MAGIC ###7. LOAD ANALYSIS into an R session 
// MAGIC 
// MAGIC 1. Using the R collect method to load the SparkSQL table into a local R session  
// MAGIC 2. List the results using the head method in R  

// COMMAND ----------

// MAGIC %r
// MAGIC library(SparkR)
// MAGIC importance_df_full  = collect(sql('SELECT * FROM importance ORDER BY importance DESC'))
// MAGIC head(importance_df_full)

// COMMAND ----------

// MAGIC %md
// MAGIC ###8. VISUALIZE ANALYSIS using R   
// MAGIC 
// MAGIC 1. Load the ggplot2 library   
// MAGIC 2. Use the R collect method to load the SparkSQL table into a local R session  
// MAGIC 3. Plot the results on a bar chart

// COMMAND ----------

// MAGIC %r
// MAGIC library(ggplot2)
// MAGIC importance_df  = collect(sql('SELECT * FROM importance ORDER BY importance DESC limit 20'))
// MAGIC ggplot(importance_df, aes(x=variable, y=importance)) + geom_bar(stat='identity') + scale_x_discrete(limits=importance_df[order(importance_df$importance), "variable"]) + coord_flip()

// COMMAND ----------

// MAGIC %md
// MAGIC ##Results interpretation
// MAGIC The plot above shows that VariantSpark has recovered the correct genotypes of this multivariate phenotype with interacting features (multiplicative and additive effects). 
// MAGIC 
// MAGIC ![Hipster-Index](https://s3.us-east-2.amazonaws.com/csiro-graphics/HipsterSignatureGraphic-new.png)
// MAGIC 
// MAGIC 1. __chr2_223034082__ (rs2218065) encoding for monobrow is the most important feature 
// MAGIC 2. a group of SNPs encoding for the MEGF10 gene (__chr5_126626044__), which is involved in Retina horizontal cell formation   
// MAGIC    as the second most important marker, explaining why hipsters prefer checked shirts
// MAGIC 3. __chr7_17284577__ (rs4410790) the marker for increased coffee consuption is ranked third  
// MAGIC 4. __chr4_54511913__ (rs4864809) the marker for beards is fourth
// MAGIC 
// MAGIC The last two are in swapped order compared to the formular of the HipsterIndex, however with 0.5 and 1 as weight they may be difficult to differentiate. 

// COMMAND ----------

// MAGIC %md
// MAGIC ### ALTERNATIVE ANALYSIS    
// MAGIC 
// MAGIC Compare the results from other tools, such as [Hail](https://hail.is).  
// MAGIC The Hail P-values for this example were computed in a [different notebook](https://docs.databricks.com/spark/latest/training/1000-genomes.html)  
// MAGIC *NOTE: To use Hail's logistic regression a different Spark version is required.* 
// MAGIC 
// MAGIC 1. Use an alternate tool (Hail) for analysis
// MAGIC 2. Load the pre-computed values using Python

// COMMAND ----------

// MAGIC %python
// MAGIC import urllib
// MAGIC urllib.urlretrieve("https://s3-us-west-1.amazonaws.com/variant-spark-pub/datasets/hipsterIndex/hail_pvals.csv", "/tmp/hail_pvals.csv")
// MAGIC dbutils.fs.cp("file:/tmp/hail_pvals.csv", "dbfs:/vs-datasets/hipsterIndex/hail_pvals.csv")
// MAGIC display(dbutils.fs.ls("dbfs:/vs-datasets/hipsterIndex/hail_pvals.csv"))

// COMMAND ----------

// MAGIC %md
// MAGIC ###1. REVIEW RESULTS FROM ALTERNATIVE ANALYSIS  
// MAGIC 
// MAGIC The list shows the result returned by Hail's logistic regression method, listing important variables in a **different** order,  
// MAGIC suggesting that complex interacting variables are better recoved using random forest.   
// MAGIC *NOTE: logistic regression results show __chr5_126626044__ came up first, instead of __chr2_223034082__.*

// COMMAND ----------

val hailDF = sqlContext.read.format("csv").option("header", "true").load("/vs-datasets/hipsterIndex/hail_pvals.csv")
display(hailDF)

// COMMAND ----------

// MAGIC %md
// MAGIC ###2. REVIEW AND PLOT COMPARATIVE ANALYSIS using R 
// MAGIC 
// MAGIC 1. Use R to setup the list of Hail p-values  
// MAGIC 2. Load the variant-spark results into a local R session  
// MAGIC 3. Prepare both sets of results for a plot  
// MAGIC 4. Plot both results using ggplot

// COMMAND ----------

// MAGIC %r
// MAGIC hail_pvals <- read.df("/vs-datasets/hipsterIndex/hail_pvals.csv", source="csv", header="true", inferSchema="true")
// MAGIC hail_pvals <- as.data.frame(hail_pvals)
// MAGIC 
// MAGIC hail_df <- aggregate(hail_pvals$pvals, by=list(hail_pvals$snp), min)
// MAGIC rownames(hail_df) <- hail_df$Group.1

// COMMAND ----------

// MAGIC %r
// MAGIC importance_df_full  = collect(sql('SELECT * FROM importance ORDER BY importance DESC'))
// MAGIC importance_df_agg <- aggregate(importance_df_full$importance, by=list(importance_df_full$variable), max)
// MAGIC rownames(importance_df_agg) <- as.vector(importance_df_agg$Group.1)
// MAGIC importance_df_agg$hail_pv <- hail_df[as.vector(importance_df_agg$Group.1), "x"]
// MAGIC 
// MAGIC importance_df_agg$color <- "b"
// MAGIC importance_df_agg[c("7_17284577", "4_54511913", "2_223034082", "5_126626044"), "color"] <- "a"
// MAGIC 
// MAGIC head(importance_df_agg)

// COMMAND ----------

// MAGIC %r
// MAGIC library(ggplot2)
// MAGIC ggplot(importance_df_agg, aes(-log10(hail_pv), sqrt(x), color=color)) + geom_point() + xlab("-log10(Hail P-value)") + ylab("sqrt(VariantSpark Importance)") + annotate("text", label=c("7_17284577", "4_54511913", "2_223034082", "5_126626044"), x=c(47,20,100,110), y=c(0.0082,0.0063,0.0124,0.0103)) + theme_bw() + theme(legend.position="none") 

// COMMAND ----------

// MAGIC %md
// MAGIC ###Summary
// MAGIC While HAIL identified the correct variables their order is not consistent with their weight in the formular. More generally, HAIL has identified a large number of variables as associated with the label that VariantSpark scores with a low Importance score. Utilizing VariantSpark random forests allows us to reduce the noise and extract the signal with the correct ordering. 

// COMMAND ----------

// MAGIC %md
// MAGIC ###Credit
// MAGIC 
// MAGIC ![VariantSpark](https://s3.us-east-2.amazonaws.com/csiro-graphics/variant-spark.png)  
// MAGIC Transformational Bioinformatics team has developed VariantSpark and put together this illustrative example. Thank you to Lynn Langit for input on the presentation of this notebook. 
