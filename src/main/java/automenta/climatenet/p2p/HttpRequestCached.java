package automenta.climatenet.p2p;


import com.squareup.okhttp.*;

import java.io.File;
import java.util.concurrent.TimeUnit;

/** https://hc.apache.org/httpcomponents-client-ga/tutorial/html/caching.html*/
public class HttpRequestCached {


    public static final HttpRequestCached the = new HttpRequestCached(new File("cache"));


    public final OkHttpClient client;
    public final Cache cache;

    public HttpRequestCached(File cacheDirectory)  {
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        cache = new Cache(cacheDirectory, cacheSize);


        client = new OkHttpClient();
        client.setCache(cache);
    }

    final CacheControl cc0 = new CacheControl.Builder().maxStale(64,TimeUnit.DAYS).onlyIfCached().build();
    final CacheControl cc = new CacheControl.Builder().maxStale(64, TimeUnit.DAYS).build();

    public Response getCached(String url) throws Exception {

        Request request = new Request.Builder()
                .url(url)
                .cacheControl(cc0)
                .build();

        return client.newCall(request).execute();
    }

    public Response getTryCacheFirst(String url) throws Exception {

        Response r;
        try {
            //try cached copy if available first
            r = getCached(url);
        }
        catch (Exception e) {
            r = null;
        }

        if (r == null || !r.isSuccessful() ) {
            Request request = new Request.Builder()
                    .url(url)
                    .cacheControl(cc)
                    .build();

            r = client.newCall(request).execute();
        }

        return r;
    }

    public Response get(String url) throws Exception {

        Request request = new Request.Builder()
                .url(url)
                .cacheControl(cc)
                .build();

        return client.newCall(request).execute();
    }

//    //https://github.com/square/okhttp/wiki/Recipes#response-caching
//    /** async request */
//    public void run(String url, Callback c) throws Exception {
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        //Response response1 = client.newCall(request).execute();
//        client.newCall(request).enqueue(c);
////        if (!response1.isSuccessful()) throw new IOException("Unexpected code " + response1);
////
////        String response1Body = response1.body().string();
////        System.out.println("Response 1 response:          " + response1);
////        System.out.println("Response 1 cache response:    " + response1.cacheResponse());
////        System.out.println("Response 1 network response:  " + response1.networkResponse());
////
////
////        Response response2 = client.newCall(request).execute();
////        if (!response2.isSuccessful()) throw new IOException("Unexpected code " + response2);
////
////        String response2Body = response2.body().string();
////        System.out.println("Response 2 response:          " + response2);
////        System.out.println("Response 2 cache response:    " + response2.cacheResponse());
////        System.out.println("Response 2 network response:  " + response2.networkResponse());
////
////        System.out.println("Response 2 equals Response 1? " + response1Body.equals(response2Body));
//    }
//
//    public static void main(String[] args) throws Exception {
//        new HttpRequestCached(new File("cache")).run(
//                "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom", new Callback() {
//
//                    @Override
//                    public void onFailure(Request request, IOException e) {
//
//                    }
//
//                    @Override
//                    public void onResponse(Response response) throws IOException {
//                        System.out.println(response.body().string());
//                    }
//                });
//    }
}
