library(timeDate)
library(randomForest)
library(stringr)
options(scipen=500)

#get data from orignal source files
dataFileStore <- read.csv(file='~/Downloads/books for DataAnalytic/walmart/stores.csv')
dataFileTrain <- read.csv(file='~/Downloads/books for DataAnalytic/walmart/train.csv')
dataFileTest <- read.csv(file='~/Downloads/books for DataAnalytic/walmart/test.csv')
dataFileFeatures <- read.csv(file='~/Downloads/books for DataAnalytic/walmart/features.csv')
submission <- read.csv(file='~/Downloads/books for DataAnalytic/walmart/sampleSubmission1.csv',header=TRUE,as.is=TRUE)

# merge stores to sales file
dataFileTrainTmp <- merge(x=dataFileTrain, y=dataFileStore, all.x=TRUE)
dataFileTestTmp <- merge(x=dataFileTest, y=dataFileStore, all.x=TRUE)
# merge features to sales file
train_full <- merge(x=dataFileTrainTmp, y=dataFileFeatures, all.x=TRUE)
test_full <- merge(x=dataFileTestTmp, y=dataFileFeatures, all.x=TRUE)
train <- as.data.frame(train_full[,c("Store","Date","IsHoliday","Dept","Weekly_Sales","Type","Size")])
test <- as.data.frame(test_full[,c("Store","Date","IsHoliday","Dept","Type","Size")])

#change Date from 1/12/12 to 2012-01-12
train$Date <- as.Date(train$Date,format = "%m/%d/%y")
train$year = as.numeric(substr(train$Date,1,4))
train$month = as.numeric(substr(train$Date,6,7))
train$day = as.numeric(substr(train$Date,9,10))
train$days = (train$month-1)*30 + train$day
train$Type = as.character(train$Type)
train$Type[train$Type=="A"]=1
train$Type[train$Type=="B"]=2
train$Type[train$Type=="C"]=3
train$IsHoliday[train$IsHoliday=="TRUE"]=1
train$IsHoliday[train$IsHoliday=="FALSE"]=0
train$dayHoliday = train$IsHoliday*train$days
train$logsales = log(4990+train$Weekly_Sales)


#Make features for test
#change Date from 1/12/12 to 2012-01-12
test$Date <- as.Date(test$Date,format = "%m/%d/%y")
test$year = as.numeric(substr(test$Date,1,4))
test$month = as.numeric(substr(test$Date,6,7))
test$day = as.numeric(substr(test$Date,9,10))
test$days = (test$month-1)*30 + test$day
test$Type = as.character(test$Type)
test$Type[test$Type=="A"]=1
test$Type[test$Type=="B"]=2
test$Type[test$Type=="C"]=3
test$IsHoliday[test$IsHoliday=="TRUE"]=1
test$IsHoliday[test$IsHoliday=="FALSE"]=0
test$dayHoliday = test$IsHoliday*test$days

rowOfSubmission = nrow(submission)
i=1

lastStore <- NULL
lastDept <-NULL
tmpModel1 <- NULL
tmpModel2 <- NULL

while(i<rowOfSubmission){
  print(i)
  print(rowOfSubmission)
  tmpId = submission$Id[i]
  tmpStr = unlist(strsplit(tmpId,"_"))
  tmpStore = tmpStr[1]
  tmpDept = tmpStr[2]
  
  dataF1 = train[train$Dept==tmpDept,]
  tmpL = nrow(dataF1[dataF1$Store==tmpStore,])
  print("tmpL:")
  print(tmpL)
  tmpF = dataF1[dataF1$IsHoliday==1,]
  dataF1 = rbind(dataF1,do.call("rbind", replicate(4, tmpF, simplify = FALSE)))
  dataF2 = dataF1[dataF1$Store==tmpStore,] 
  
  testF1 = test[test$Dept==tmpDept,]
  testF1 = testF1[testF1$Store==tmpStore,]
  testRows = nrow(testF1)

  if (tmpL<10) { 
    if((!is.null(tmpModel1))&&((lastStore==tmpStore)&&(lastDept==tmpDept))){
      tmpModel = tmpModel1
    }
    else{
      tmpModel =  randomForest(logsales~Size+Type+ year + month + day + days + dayHoliday, 
                               ntree=4800, replace=TRUE, mtry=4, data=dataF1)
      tmpModel1 = tmpModel
    }
  }
  else{ 
    if((!is.null(tmpModel2))&&((lastStore==tmpStore)&&(lastDept==tmpDept))){
      tmpModel = tmpModel2
    }
    else{
      tmpModel =  randomForest(logsales ~ year + month + day + days + dayHoliday, 
                               ntree=4800, replace=TRUE, mtry=3, data=dataF2)
      tmpModel2 = tmpModel
    }
  }    
  lastStore<-tmpStore
  print("============last store:")
  print(tmpStore)
  lastDept<-tmpDept
  print("============last dept:")
  print(tmpDept)

tmpP = exp(predict(tmpModel,testF1))-4990 
j = i + testRows - 1 
submission$Weekly_Sales[i:j] = tmpP
i = j+1
}

write.table(x=submission,
            file='~/Downloads/books for DataAnalytic/walmart/outputFinal3.csv',
            sep=',', row.names=FALSE, quote=FALSE)


