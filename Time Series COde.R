library(forecast)

printForecasts = function(sid, deptid, vals) {
  numVals = length(vals)
  df = data.frame(Id=rep("",numVals), Weekly_Sales=rep(0,numVals), stringsAsFactors = F )
  stDate = as.Date("2012-11-02")
  for(i in 1:length(vals)) {
    dateString = paste(sid,deptid,as.character(stDate),sep="_")
    df[i,1]=dateString
    df[i,2]=vals[i]
    stDate = stDate + 7
  }
  return(df)
}

modelHoltWinters= function(sid, deptid, newdata) {
  tryCatch({
    ds11 = newdata[newdata$Store==sid & newdata$Dept==deptid,]$Weekly_Sales
    ts11 = ts(ds11, frequency = 52)
    hw11model = HoltWinters(ts11)
    hw11forecasts = forecast.HoltWinters(hw11model, h=39)
    pointForecasts= as.data.frame(hw11forecasts)$"Point Forecast"
    
    print(paste(sid,deptid))
    return (printForecasts(sid,deptid,pointForecasts))    
  }, error=function(e){ 
    ds11 = newdata[newdata$Store==sid & newdata$Dept==deptid,]$Weekly_Sales
    print(paste(sid,deptid," --> FAIL: ", nrow(newdata), length(ds11)))
    if(length(ds11)!=143) {
      return (printForecasts(sid,deptid,rep(0,39)))
    }
    return (printForecasts(sid,deptid,ds11[92:130]))
  } 
  )
}

newdata = read.csv('/Users/aparnakumar/Downloads/Denormalised-US.csv')
forecastDF = data.frame(Id="", Weekly_Sales=0, stringsAsFactors = F)
numStores = length(unique(newdata$Store))

for(sid in 1:numStores) {
  depts = unique( newdata[newdata$Store==sid,]$Dept )  
  for(i in 1:length(depts) ) {
    deptid = depts[i]
    df = modelHoltWinters(sid,deptid, newdata)
    forecastDF = rbind(forecastDF, df)
  }
}

write.csv(forecastDF[-1,], "Desktop/Walmart.HoltWinters.csv", quote=F, row.names=F)


read.csv("/Users/aparnakumar/Desktop/test.csv")
testdf$Id = paste(testdf$Store,"_",testdf$Dept,"_",testdf$Date,sep="")
m=merge(forecastDF,testdf,by="Id",all.y=T)[,1:2]
write.csv(m,"/Users/aparnakumar/Desktop/upload.csv",row.names=F,quote=F)