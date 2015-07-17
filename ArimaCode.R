modelarima2 = function(sid, deptid, newdata) {
  tryCatch({
    ar11 = newdata[newdata$Store==sid & newdata$Dept==deptid,]$Weekly_Sales
    ts11 = ts(ar11, frequency = 52)
    ar11model = auto.arima(ts11)
    ar11forecasts = forecast.Arima(ar11model, h=39)
    arimaForecasts= as.data.frame(ar11forecasts)$"Point Forecast"
    
    print(paste(sid,deptid))
    return (printForecasts(sid,deptid,arimaForecasts))    
  }, error=function(e){ 
    ar11 = newdata[newdata$Store==sid & newdata$Dept==deptid,]$Weekly_Sales
    print(paste(sid,deptid," --> FAIL: ", nrow(newdata), length(ar11)))
    if(length(ar11)!=143) {
      return (printForecasts(sid,deptid,rep(0,39)))
    }
    return (printForecasts(sid,deptid,ar11[92:130]))
  } 
  )
}

newdata = read.csv('/Users/aparnakumar/Downloads/Denormalised-US.csv')
forecastArima2 = data.frame(Id="", Weekly_Sales=0, stringsAsFactors = F)
numStores = length(unique(newdata$Store))

for(sid in 1:numStores) {
  depts = unique( newdata[newdata$Store==sid,]$Dept )  
  for(i in 1:length(depts) ) {
    deptid = depts[i]
    df = modelarima(sid,deptid, newdata)
    forecastArima2 = rbind(forecastArima2, df)
  }
}

write.csv(forecastArima2[-1,], "~/Desktop/Walmart.Arima.csv", quote=F, row.names=F)


testar <- read.csv("~/Desktop/test.csv")
testar$Id = paste(testar$Store,"_",testar$Dept,"_",testar$Date,sep="")
m=merge(forecastArima2,testar,by="Id",all.y=T)[,1:2]
write.csv(m,"~/Desktop/upload.csv",row.names=F,quote=F)