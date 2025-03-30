package com.andisec.comparison.service;

import com.andisec.comparison.model.DataDiff;
import com.andisec.comparison.model.DiffResult;
import com.andisec.comparison.model.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.math.BigInteger;

@Service
public class FileCompareService {

    private final RestTemplate restTemplate = new RestTemplate();


    public DiffResult compareServers(String server1, String server2) {
        DiffResult diffResult = new DiffResult();

        Map<String, FileInfo> files1 = scanServer(server1, "");
        Map<String, FileInfo> files2 = scanServer(server2, "");
        diffResult.setNum1(files1.size());
        diffResult.setNum2(files2.size());
        List<DataDiff> dataDiffs = new ArrayList<>();
        // 找出只在server1存在的文件
        Set<String> onlyInServer1 = new HashSet<>(files1.keySet());
        onlyInServer1.removeAll(files2.keySet());
        for (String path : onlyInServer1) {
            dataDiffs.add(new DataDiff(URLDecoder.decode(path, StandardCharsets.UTF_8), "缺失", "不一致（服务器2中不存在）"));
        }

        // 找出只在server2存在的文件
        Set<String> onlyInServer2 = new HashSet<>(files2.keySet());
        onlyInServer2.removeAll(files1.keySet());
        for (String path : onlyInServer2) {
            dataDiffs.add(new DataDiff("缺失", URLDecoder.decode(path, StandardCharsets.UTF_8), "不一致（服务器1中不存在）"));
        }

        // 找出共同文件但内容不同的
        Set<String> commonFiles = new HashSet<>(files1.keySet());
        commonFiles.retainAll(files2.keySet());

        for (String file : commonFiles) {
            if (!files1.get(file).isDirectory() && !files2.get(file).isDirectory()) {
                if (!files1.get(file).getMd5().equals(files2.get(file).getMd5())) {
                    dataDiffs.add(new DataDiff(URLDecoder.decode(file, StandardCharsets.UTF_8), URLDecoder.decode(file, StandardCharsets.UTF_8), "不一致（MD5不同）"));
                } else {
                    dataDiffs.add(new DataDiff(URLDecoder.decode(file, StandardCharsets.UTF_8), URLDecoder.decode(file, StandardCharsets.UTF_8), "一致"));
                }
            } else {
                dataDiffs.add(new DataDiff(URLDecoder.decode(file, StandardCharsets.UTF_8), URLDecoder.decode(file, StandardCharsets.UTF_8), "一致"));
            }
        }
        diffResult.setDataDiffs(dataDiffs);
        return diffResult;
    }

    private Map<String, FileInfo> scanServer(String baseUrl, String path) {
        Map<String, FileInfo> fileMap = new HashMap<>();
        scanDirectory(baseUrl, path, fileMap);
        return fileMap;
    }

    private void scanDirectory(String baseUrl, String path, Map<String, FileInfo> fileMap) {
        String url = baseUrl + (path.isEmpty() ? "" : "/" + path);

        try {
            String html = restTemplate.getForObject(url, String.class);
            Document doc = Jsoup.parse(html);
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String href = link.attr("href");
                if (href.equals("../")) continue;

                String fullPath = path.isEmpty() ? href : path + href;
                String itemUrl = baseUrl + "/" + fullPath;

                if (href.endsWith("/")) {
                    // 目录
                    fileMap.put(fullPath, new FileInfo(fullPath, true, null));
                    scanDirectory(baseUrl, fullPath, fileMap);
                } else {
                    // 文件
                    byte[] content = restTemplate.getForObject(itemUrl, byte[].class);
                    String md5 = calculateMD5(content);
                    fileMap.put(fullPath, new FileInfo(fullPath, false, md5));
                }
            }
        } catch (Exception e) {
            // 可能是文件而不是目录
            if (!path.isEmpty() && !path.endsWith("/")) {
                byte[] content = restTemplate.getForObject(url, byte[].class);
                String md5 = calculateMD5(content);
                fileMap.put(path, new FileInfo(path, false, md5));
            }
        }
    }

    private String calculateMD5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content);
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(16);
        } catch (Exception e) {
            throw new RuntimeException("MD5 calculation failed", e);
        }
    }
}
