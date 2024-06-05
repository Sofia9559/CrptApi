package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@FieldDefaults(level = AccessLevel.PRIVATE)

public class CrptApi {
        final TimeUnit timeUnit;
        final int requestLimit;
        final AtomicInteger requestCounter = new AtomicInteger(0);
        Date lastResetTime = new Date();

        public CrptApi(TimeUnit timeUnit, int requestLimit) {
            this.timeUnit = timeUnit;
            this.requestLimit = requestLimit;
        }

        public void createDocument(Object document, String signature)
                throws IOException, InterruptedException, ParseException {

            synchronized (this) {
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - lastResetTime.getTime();
                if (timePassed >= timeUnit.toMillis(1)) {
                    requestCounter.set(0);
                    lastResetTime = new Date(currentTime);
                }

                while (requestCounter.get() >= requestLimit) {
                    wait(timeUnit.toMillis(1) - timePassed);
                    currentTime = System.currentTimeMillis();
                    timePassed = currentTime - lastResetTime.getTime();

                    if (timePassed >= timeUnit.toMillis(1)) {
                        requestCounter.set(0);
                        lastResetTime = new Date(currentTime);
                    }
                }

                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(document);

                StringEntity entity = new StringEntity(json);
                httpPost.setEntity(entity);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Signature", signature);

                CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost);
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, "UTF-8");

                requestCounter.incrementAndGet();
                System.out.println("Document creation response: " + responseString);
            }
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Data

    public static class Description {
        String participantInn;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)

    public static class Product {

        String certificate_document;
        Date certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        Date production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Data

    public static class Specification {

        String doc_id;
        String doc_status;
        DocType doc_type;
        boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        Date production_date;
        String production_type;
        Date reg_date;
        String reg_number;
        List<Product> products;
        Description description;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    public enum DocType {
        LP_INTRODUCE_GOODS;
    }



    public static void main( String[] args )
            throws IOException, InterruptedException, ParseException {

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        Object document = new Specification();
        String signature = "signature";

        crptApi.createDocument(document, signature);
    }
}
