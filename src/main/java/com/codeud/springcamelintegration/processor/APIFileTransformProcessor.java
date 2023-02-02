package com.codeud.springcamelintegration.processor;

import com.codeud.springcamelintegration.util.FileExchangeProcessorUtil;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class APIFileTransformProcessor {
    @Autowired
    FileExchangeProcessorUtil fileExchangeProcessorUtil;

    public void csvJavaTransformation(Exchange exchange) {
        List<List<String>> csvRecordList = (List<List<String>>) exchange.getIn().getBody();
        fileExchangeProcessorUtil.processFileExchange(csvRecordList, exchange);
    }

    public void jsonJavaTransformation(Exchange exchange) {
        LinkedHashMap<String, Object> jsonRecordMap = (LinkedHashMap<String, Object>)exchange.getIn().getBody();
        fileExchangeProcessorUtil.processFileExchange(jsonRecordMap, exchange);
    }

    public void xmlJavaTransformation(Exchange exchange) {
        HashMap<String, Object> xmlRecordMap = (HashMap<String, Object>) exchange.getIn().getBody();
        fileExchangeProcessorUtil.processFileExchange(xmlRecordMap, exchange);
    }


}
