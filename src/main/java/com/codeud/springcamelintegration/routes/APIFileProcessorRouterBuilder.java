package com.codeud.springcamelintegration.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.YAMLDataFormat;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class APIFileProcessorRouterBuilder extends RouteBuilder {
    YAMLDataFormat yaml = new YAMLDataFormat();

    @Value("${rabbitmq.server}")
    private String rabbitServer;
    @Value("${rest.webserver}")
    private String restWebServer;
    @Value("${rest.serverhost}")
    private String restServerHost;
    @Value("${rest.serverport}")
    private String restServerPort;

    @Override
    public void configure() {

        //rest service configuration
        restConfiguration()
                .host(restServerHost)
                .port(restServerPort)
                .component(restWebServer)
                .bindingMode(RestBindingMode.auto);

        //Invoking rest API with route id as rest-router
        rest("/file/{fileName}")
                .post()
                .routeId("rest-router")
                .to("direct:content");

        //Control flow based on content type from Rest API between XML, JSON and CSV type
        from("direct:content")
                .log("content-route")
                .routeId("content-route")
                .choice()
                    .when(header("Content-Type").isEqualTo("text/csv"))
                        .log("routing to direct:csv")
                        .to("direct:csv")
                    .when(header("Content-Type").isEqualTo("application/xml"))
                        .log("routing to direct:xml")
                        .to("direct:xml")
                    .when(header("Content-Type").isEqualTo("application/json"))
                        .log("routing to direct:json")
                        .to("direct:json")
                    .otherwise()
                        .log("routing to direct: content-error")
                        .log("routing to direct:error")
                        .to("direct:error");

        //CSV Content
        from("direct:csv").routeId("csv-route")
                .doTry()
                    .log("csv-route")
                    .unmarshal().csv()
                    .bean("APIFileTransformProcessor", "csvJavaTransformation")
                    .marshal(yaml)
                    .to("rabbitmq://" + rabbitServer + "/jsonExchange?queue=jsonQueue&routingKey=jsonRoutingKey&autoDelete=false")
                .doCatch(Exception.class)
                    .log("csv-route-error")
                    .process(exchange -> {
                        String fileName = (String) exchange.getIn().getHeader("fileName");
                        String errorFileName = fileName.substring(0, fileName.lastIndexOf(".")) + "-" + System.currentTimeMillis() + "-error.txt";
                        exchange.getIn().setHeader("errFileName", errorFileName);
                    })
                    .to("rabbitmq://" + rabbitServer + "/deadLetterExchange?queue=deadLetterQueue&routingKey=deadLetterRoutingKey")
                .doFinally()
                    .log("csv-route-finally")
                    .to("file:outputs?fileName=${header.fileName}-${header.currentTimeStamp}.yaml")
                .end();

        //XML Content
        from("direct:xml").routeId("xml-route")
                .doTry()
                    .log("xml-route")
                    .unmarshal().jacksonXml()
                    .bean("APIFileTransformProcessor", "xmlJavaTransformation")
                    .marshal(yaml)
                    .to("rabbitmq://"+ rabbitServer + "/jsonExchange?queue=jsonQueue&routingKey=jsonRoutingKey&autoDelete=false")
                .doCatch(Exception.class)
                    .log("xml-route-error")
                    .process(exchange -> {
                        String fileName = (String) exchange.getIn().getHeader("fileName");
                        String errorFileName = fileName.substring(0, fileName.lastIndexOf(".")) + "-" + System.currentTimeMillis() + "-error.txt";
                        exchange.getIn().setHeader("errFileName", errorFileName);
                    })
                    .to("rabbitmq://"+ rabbitServer +"/deadLetterExchange?queue=deadLetterQueue&routingKey=deadLetterRoutingKey")
                .doFinally()
                    .log("xml-route-finally")
                    .to("file:outputs?fileName=${header.fileName}-${header.currentTimeStamp}.yaml")
                .end();

        //JSON Content
        from("direct:json").routeId("json-route")
                .doTry()
                    .log("json-route")
                    .bean("APIFileTransformProcessor", "jsonJavaTransformation")
                    .marshal(yaml)
                    .to("rabbitmq://"+ rabbitServer +"/jsonExchange?queue=jsonQueue&routingKey=jsonRoutingKey&autoDelete=false")
                .doCatch(Exception.class)
                    .log("json-route-error")
                     .process(exchange -> {
                         String fileName = (String) exchange.getIn().getHeader("fileName");
                         String errorFileName = fileName.substring(0, fileName.lastIndexOf(".")) + "-" + System.currentTimeMillis() + "-error.txt";
                         exchange.getIn().setHeader("errFileName", errorFileName);
                     })
                    .to("rabbitmq://"+ rabbitServer +"/deadLetterExchange?queue=deadLetterQueue&routingKey=deadLetterRoutingKey")
                .doFinally()
                         .log("json-route-finally")
                         .to("file:outputs?fileName=${header.fileName}-${header.currentTimeStamp}.yaml")
                .end();
    }
}
