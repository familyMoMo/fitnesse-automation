package self.family.test;

import com.jayway.jsonpath.JsonPath;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import self.family.entry.HttpDeleteWithBody;
import self.family.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by family on 15/11/16.
 */
public class APIController {

    //设置证书,防止请求时报错
    static {
        System.setProperty("javax.net.ssl.trustStore", "/Users/family/IdeaProjects/workspace_self/fitnesse-test/jssecacerts");
    }

    private final static CloseableHttpClient httpClient = HttpClients.createDefault();
    private HttpResponse httpResponse;
    private String strResponse;
    private List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
    private List<String> checkParameters = new ArrayList<String>();
    private List<String> expectParameters = new ArrayList<String>();
    private Header[] headers;

    /**
     * 设置请求参数
     * @param paramMap 请求参数
     */
    public void setParameters(Map<String, String> paramMap){
        //Todo 上传文件的处理
        for (Entry<String, String> entry : paramMap.entrySet())
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));

    }

    /**
     * 设置请求头
     * @param headerMap 请求头,value是ArrayList
     */
    public void setHeaders(Map<String, String> headerMap){
        List<Header> headerList = new ArrayList<Header>();
        for (Entry<String, String> entry : headerMap.entrySet()){
            try {
                for(String value : (List<String>)JsonUtil.fromJson(entry.getValue(), List.class))
                    headerList.add(new BasicHeader(entry.getKey(), value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final int size =  headerList.size();
        headers = (Header[])headerList.toArray(new Header[size]);
    }

    /**
     * Get请求
     * @param url 请求地址
     */
    public void doGet(String url){
        HttpGet httpGet = new HttpGet(url);
        if (headers != null)
            httpGet.setHeaders(headers);
        try {
            httpResponse = httpClient.execute(httpGet);
//            while (httpResponse.getStatusLine().getStatusCode() == 301 || httpResponse.getStatusLine().getStatusCode() == 302){
//                httpResponse = httpClient.execute(new HttpGet(httpResponse.getLastHeader("Location").getValue()));
//            }
            strResponse = EntityUtils.toString(httpResponse.getEntity(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Post请求
     * @param url 请求地址
     */
    public void doPost(String url){
        HttpPost httpPost = new HttpPost(url);
        if (headers != null)
            httpPost.setHeaders(headers);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(parameters, "utf8"));
            httpResponse = httpClient.execute(httpPost);
            while (httpResponse.getStatusLine().getStatusCode() == 301 || httpResponse.getStatusLine().getStatusCode() == 302){
                httpResponse = httpClient.execute(new HttpGet(httpResponse.getLastHeader("Location").getValue()));
            }
            strResponse = EntityUtils.toString(httpResponse.getEntity(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param url 请求地址
     */
    public void doDelete(String url){
//        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
////        StringEntity entity = new StringEntity("reason=test", ContentType.APPLICATION_FORM_URLENCODED);
//        if (headers != null)
//            httpDelete.setHeaders(headers);
//        try {
//            httpDelete.setEntity(new UrlEncodedFormEntity(parameters,"utf8"));
//            httpResponse = httpClient.execute(httpDelete);
//            strResponse = EntityUtils.toString(httpResponse.getEntity(), "utf8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        HttpDelete httpDelete = new HttpDelete(url);
        if(headers != null)
            httpDelete.setHeaders(headers);
        try {
            httpResponse = httpClient.execute(httpDelete);
            strResponse = EntityUtils.toString(httpResponse.getEntity(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示请求返回结果
     * @return 返回response的entity
     */
    public String showResponse(){
        return strResponse;
    }

    /**
     * 设置断言列表
     * @param paramList 断言列表,若为Json需JsonPath
     */
    public void setCheckParameters(ArrayList<String> checkParamList){
        checkParameters = checkParamList;
    }

    /**
     * @param expectParamList 期望值列表
     */
    public void setExpectParameters(ArrayList<String> expectParamList) {
        expectParameters = expectParamList;
    }

    /**
     * 检查请求返回的http code
     * @return 返回状态码
     */
    public String checkStatusCode(){
        return httpResponse.getStatusLine().getStatusCode() + "";
    }

    /**
     * 检查请求返回的Json串
     * @return 返回Json串
     */
    public String checkJsonResponse(){
        ArrayList<String> outJsonResponseList = new ArrayList<String>();
        for (String param : checkParameters)
            outJsonResponseList.add(param + "=" + JsonPath.read(strResponse, param).toString());
        if (expectParameters.size() == 0) {
            String outJsonResponse = "";
            for (String strOutJsonResponse : outJsonResponseList)
                outJsonResponse += strOutJsonResponse + ",";
            if (!outJsonResponse.equals(""))
                outJsonResponse = outJsonResponse.substring(0, outJsonResponse.length() - 1);
            return outJsonResponse;
        } else {
            if (expectParameters.containsAll(outJsonResponseList))
                return "YES";
            else
                return "NO";
        }
    }

    /**
     * 检查请求返回的Html
     * @return 返回check结果
     */
    public String checkHtmlResponse(){
        String result = "YES";
        if (!strResponse.equals("")) {
            for (String param : checkParameters) {
                if (strResponse.contains(param)){
                    continue;
                } else {
                    result = "NO";
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 获取response Json 的结果
     * @param key JsonPath
     * @return 返回key对应的value值
     */
    public String getJsonValue(String key){
        return JsonPath.parse(strResponse).read(key).toString();
    }


    /**
     * @param regexList 正则表达式参数列表,0:正则表达式;1:模板(默认为1);2:匹配数字(默认为1,0为随机)
     * @return
     */
    public String getRegexValue(ArrayList<String> regexList) {
        String regex = regexList.get(0);
        int template = 1;
        int index = 1;
        if (regexList.size() == 3){
            template = Integer.parseInt(regexList.get(1));
            index = Integer.parseInt(regexList.get(2));
        }
        Pattern compile = Pattern.compile(regex.replaceAll("\\\\", "\\\\\\\\"));
        Matcher matcher = compile.matcher(strResponse);
        ArrayList<String> tmps = new ArrayList<String>();
        while (matcher.find()) {
            tmps.add(matcher.group(template));
        }
        if (index == 0) {
            Random random = new Random();
            index = random.nextInt(tmps.size());
        }
        if (tmps.size() >= index) {
            return tmps.get(index - 1);
        }
        return null;
    }

    /**
     * 获取response Headers 的Json串
     * @param headerName Header名
     * @return 该headerName对应的ArrayList的Json串
     */
    public String getHeaderValue(String headerName){
        Header[] responseHeaders = httpResponse.getHeaders(headerName);
        ArrayList<String> headerValueList = new ArrayList<String>();
        for (Header header : responseHeaders)
            headerValueList.add(header.getValue());
        try {
            return JsonUtil.toJson(headerValueList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
