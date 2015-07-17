#####################################################################
# Description: Train the model and generate the data predictions
# 
# Kaggle contest description, rules and data: 
# http://www.kaggle.com/c/walmart-recruiting-store-sales-forecasting
# 
# Author: Vignan Uppugandla <vignan.uppugandla@sv.cmu.edu> 
#####################################################################

#set the working directory before start of any operation
setwd('F:/DataFiles/Competition/data')

#gathering the required packages
require(gbm)
require(dplyr)

#getting all the departments in the test data
deps<-sort(unique(test$Dept))

#Loop foriterating through the departments
#Also training the model and predicting for each department with the model
for(dept in deps){
 
 #getting the train data for that particular dept
 train.dep=train[train$Dept==dept,]
 row.names(train.dep) <- seq(1, dim(train.dep)[1])
 
 #getting the test data for that particular dept
 test.dep=test[test$Dept==dept,]
 row.names(test.dep) <- seq(1, dim(test.dep)[1])
 
 #Separating the weekly sales from the train data
 Weekly_Sales.dep<-train.dep$Weekly_Sales
 train.dep<-select(train.dep,-Weekly_Sales)
 end_trn.dep=nrow(train.dep)
 
 
 #combined data with train+test
 all.dep=rbind(train.dep,test.dep)
 end.dep=nrow(all.dep)
 
 #Removing some of the columns which are not required
 all.dep<-select(all.dep,-Dept)
 all.dep<-select(all.dep,-Date)
 all.dep<-select(all.dep,-day_index)
 all.dep<-select(all.dep,-IsHoliday,-Holiday,-last_holiday,-next_holiday) 
 
 
 #gbm
 ntrees=5000
 
 #training the model with gbm.fit
 model.dep=gbm.fit(
  x=all.dep[1:end_trn.dep,]
  , y=Weekly_Sales.dep
  , distribution = c('laplace','gaussian')[2]
  , n.trees = ntrees
  , shrinkage =0.1
  , interaction.depth = 10
  , n.minobsinnode = 2
  , bag.fraction =0.7
  , nTrain = round(end_trn.dep*0.8)#805 for train
  , verbose = TRUE
 )
 
 # performance of the model which gives the optimum iterations required
 gbm.perf(model.dep)
 
 # Summary of the model which finds the influence of each variable on the model
 summary(model.dep)
 
 #forecast/prediction on the test data
 testPredictions.dep=predict(object=model.dep,newdata=all.dep[(end_trn.dep+1):end.dep,]
                             , n.trees=gbm.perf(model.dep,plot.it=FALSE)
                             , type="response")
 #forecast/prediction on the train data
 trainPredictions.dep=predict(object=model.dep,newdata=all.dep[1:end_trn.dep,]
                              , n.trees=gbm.perf(model.dep,plot.it=FALSE)
                              , type="response")
 
 #binding the data onto a dataframe
 if(dept==1){
 sampleSubmission<-data.frame(Id=paste(test.dep$Store,test.dep$Dept,test.dep$Date,sep='_')
                                  , Weekly_Sales=testPredictions.dep)
 row.names(sampleSubmission) <- seq(1, dim(sampleSubmission)[1])
 }
 
 #binding the data onto a dataframe
 if(dept!=1){
  sampleSubmission=rbind(sampleSubmission,data.frame(Id=paste(test.dep$Store,test.dep$Dept,test.dep$Date,sep='_')
                           , Weekly_Sales=testPredictions.dep))
}

#writing the data into a csv file
write.csv(sampleSubmission,file="sampleSubmission_dep.csv")
}


#script used for generating plots--used inside the loop randomly
filePath <- paste('F:/DataFiles/Plot','/',store,sep='')
dir.create(filePath, showWarnings=FALSE, recursive=TRUE)

png(filename=paste(filePath,'/Store',store,'Dept', dept,'.png',sep=''))

plot(train2$day_index, train2$Weekly_Sales,
     type='l', col='black', xlim=c(1,180),
     main=paste('Store: ',store,' Dept: ', dept,sep=' '),
     xlab='Date Index', ylab='Weekly Sales')
points(c(train2$day_index, test2$day_index),c(trainPredictions.dep,testPredictions.dep),
       type='l', col='red')

dev.off()
