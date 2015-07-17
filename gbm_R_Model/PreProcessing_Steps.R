#####################################################################
# Description: Clean the data and generate features.
# 
# Kaggle contest description, rules and data: 
# http://www.kaggle.com/c/walmart-recruiting-store-sales-forecasting
# 
# Author: Vignan Uppugandla <vignan.uppugandla@sv.cmu.edu> 
#####################################################################

#set the working directory before start of any operation
setwd('F:/DataFiles/Competition/data')
ls()

#gathering the required packages
require(reshape)

#Reading all the files
store <- read.csv(file='F:/DataFiles/Old/stores.csv',header=T)
features <- read.csv(file='F:/DataFiles/Old/features.csv',header=T)
train <- read.csv(file='F:/DataFiles/Old/train.csv',header=T)
test <- read.csv(file='F:/DataFiles/Old/test.csv',header=T)
sampleSubmission<-read.csv(file='F:/DataFiles/old/sampleSubmission.csv')


#Merging store with train & test
train<-merge(x=train,y=store,all.x=T)
test<-merge(x=test,y=store,all.x=T)

#Merging features with train and test
train<-merge(x=train,y=features,x.all=T)
test<-merge(x=test,y=features,x.all=T)

#coverting date into "yyyy-mm-dd"
head(train$Date)
train$Date<-as.Date(train$Date,format="%m/%d/%Y")
test$Date<-as.Date(test$Date,format="%m/%d/%Y")
features$Date<-as.Date(features$Date,format="%m/%d/%Y")

#Date to character
train$Date<-as.character(train$Date)
test$Date<-as.character(test$Date)
features$Date<-as.character(features$Date)

#Generating "Number of days to beginning"
baseline_date<-as.Date('2010-02-05')
train$days_to_base<-as.numeric(as.Date(train$Date)-baseline_date)
test$days_to_base<-as.numeric(as.Date(test$Date)-baseline_date)

#Generating "day index for each data in increasing order"
all_dates<-sort(unique(features$Date))
train$day_index<-sapply(train$Date,function(d)which(d==all_dates))
test$day_index<-sapply(test$Date,function(d)which(d==all_dates))


#Splitting date into year, month, day
date<-strsplit(train$Date,'-')
date<-as.numeric(unlist(date))
date<-matrix(date,dim(train)[1],3,byrow=T)
train$year<-date[,1]
train$month<-date[,2]
train$day<-date[,3]

date<-strsplit(test$Date,'-')
date<-as.numeric(unlist(date))
date<-matrix(date,dim(test)[1],3,byrow=T)
test$year<-date[,1]
test$month<-date[,2]
test$day<-date[,3]

#Generating binary values(0 or 1) for IsHoliday
train$holiday_binary<-ifelse(train$IsHoliday==TRUE,1,0)
test$holiday_binary<-ifelse(test$IsHoliday==TRUE,1,0)

#Arranging the columns in a particular order for convenience
train<-train[,c('Store','Dept','Type','Size','Date','year','month','day','days_to_base','day_index','IsHoliday','holiday_binary','Temperature','Fuel_Price','CPI','Unemployment','MarkDown1','MarkDown2','MarkDown3','MarkDown4','MarkDown5','Weekly_Sales')]
test<-test[,c('Store','Dept','Type','Size','Date','year','month','day','days_to_base','day_index','IsHoliday','holiday_binary','Temperature','Fuel_Price','CPI','Unemployment','MarkDown1','MarkDown2','MarkDown3','MarkDown4','MarkDown5')]

#Sorting the data by Store, Dept, Date
vars <- c('Store', 'Dept', 'Date')
## train
train <- sort_df(train, vars)
row.names(train) <- seq(1, dim(train)[1]) # just for nice display
## test
test <- sort_df(test, vars)
row.names(test) <- seq(1, dim(test)[1]) # just for nice display

#Assigning a name for the holiday
Super_Bowl <- c('2010-02-12', '2011-02-11', '2012-02-10', '2013-02-08')
Labor_Day <- c('2010-09-10', '2011-09-09', '2012-09-07', '2013-09-06')
Thanksgiving <- c('2010-11-26', '2011-11-25', '2012-11-23', '2013-11-29')
Christmas <- c('2010-12-31', '2011-12-30', '2012-12-28', '2013-12-27')
Holidays <- data.frame(Super_Bowl=Super_Bowl,
                       Labor_Day=Labor_Day,
                       Thanksgiving=Thanksgiving,
                       Christmas=Christmas)

#function to return the holiday name for the date
func <- function(d, Holidays){
 d<-as.character(d)
 return(colnames(Holidays)[which(Holidays == d, arr.ind=TRUE)[2]])
}

#train
train$Holiday <- rep('No', dim(train)[1])
train$Holiday[train$IsHoliday == TRUE] <- sapply(train$Date[train$IsHoliday == TRUE],
                                                     function(d)func(d, Holidays))
#test
test$Holiday <- rep('No', dim(test)[1])
test$Holiday[test$IsHoliday == TRUE] <- sapply(test$Date[test$IsHoliday == TRUE],
                                                 function(d)func(d, Holidays))

#number of days from/to before/next holiday
HolidayTimeLine <- rbind(Super_Bowl,Labor_Day,Thanksgiving,Christmas)
HolidayTimeLine <- unlist(HolidayTimeLine)
Christmas_2009 <- ('2009-12-25')
HolidayTimeLine <- c(Christmas_2009, HolidayTimeLine)
holiday_names <- c('Super_Bowl', 'Labor_Day', 'Thanksgiving', 'Christmas')
holiday_names <- c('Christmas', rep(holiday_names, 4))
# convert to Date class
HolidayTimeLine <- as.Date(HolidayTimeLine)

func <- function(d, HolidayTimeLine, holiday_names){
 dif <- as.numeric(d-HolidayTimeLine)
 ind <- which.min(abs(dif))
 # d comes after the closest date
 if(dif[ind] > 0){
  last_holiday_ind <- ind
 }else{
  last_holiday_ind <- ind - 1
 }  
 last_holiday <- holiday_names[last_holiday_ind]
 days_from_last_holiday <- abs(dif[last_holiday_ind])
 next_holiday <- holiday_names[last_holiday_ind+1]
 days_to_next_holiday <- abs(dif[last_holiday_ind+1])
 # while days_from_last_holiday/days_to_next_holiday are numeric, they are
 # coerced to characters due to the fact to last_holiday/next_holiday are
 # characters and the use of c() function.
 # Thus, outside of this function, we have to convert them back to numeric
 # if we want to use them in numerical computation.
 return(c(last_holiday, days_from_last_holiday,
          next_holiday, days_to_next_holiday))
}

#find the number of days
results <- sapply(as.Date(train$Date), function(d)func(d, HolidayTimeLine, holiday_names))

train$last_holiday <- results[1,]
train$days_from_last_holiday <- as.numeric(results[2,])

train$next_holiday <- results[3,]
train$days_to_next_holiday <- as.numeric(results[4,])

results <- sapply(as.Date(test$Date), function(d)func(d, HolidayTimeLine, holiday_names))

test$last_holiday <- results[1,]
test$days_from_last_holiday <- as.numeric(results[2,])

test$next_holiday <- results[3,]
test$days_to_next_holiday <- as.numeric(results[4,])

#Findinf the Weekly Sales of last year
func <- function(d, df, offset){
 # find the closest date to d a year ago in dfTrain
 last_year_ind <- which(df$day_index == (d - offset))
 if(length(last_year_ind)>0){
  return(df$Weekly_Sales[last_year_ind])
 }else{
  return(NA)
 }
}

## temp variable
test2 <- test
test2$Weekly_Sales <- rep(NA, dim(test2)[1])
dfAll <- rbind(train, test2)

## train
# offset = 51
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index<=143],
                                          function(d)func(d, df, offset=51))))
results <- unlist(results)
train$Last_Year_Sales51 <- results
# offset = 52
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index<=143],
                                          function(d)func(d, df, offset=52))))
results <- unlist(results)
train$Last_Year_Sales52 <- results
# offset = 53
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index<=143],
                                          function(d)func(d, df, offset=53))))
results <- unlist(results)
train$Last_Year_Sales53 <- results

## test
# offset = 51
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index>143],
                                          function(d)func(d, df, offset=51))))
results <- unlist(results)
test$Last_Year_Sales51 <- results
# offset = 52
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index>143],
                                          function(d)func(d, df, offset=52))))
results <- unlist(results)
test$Last_Year_Sales52 <- results
# offset = 53
results <- with(dfAll,
                by(data=dfAll,
                   INDICES=list(Store, Dept),
                   FUN=function(df)sapply(df$day_index[df$day_index>143],
                                          function(d)func(d, df, offset=53))))
results <- unlist(results)
test$Last_Year_Sales53 <- results

#### Convert variables to factors
factor.vars <- c('Store', 'Dept', 'Type', 'month',
                 'IsHoliday', 'Holiday', 'last_holiday', 'next_holiday')
Ftrain<-train
Ftest<-test
for(v in factor.vars){
 Ftrain[,v] <- as.factor(train[,v])
 Ftest[,v] <- as.factor(test[,v])
}

### save data
rm(Holidays)
rm(test2,dfAll,Christmas,Christmas_2009,HolidayTimeLine,Labor_Day,Super_Bowl,Thanksgiving,all_Dates,all_dates,baseline_date,v,vars)
rm(holiday_names,factor.vars)
save.image("F:/DataFiles/Competition/data/Processed_Walmart_Data.RData")
