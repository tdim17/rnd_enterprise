package com.volta.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volta.core.ConfigurationReader;
import com.volta.pojo.ExportItem;
import com.volta.pojo.LinkCheckItem;
import com.volta.pojo.VerifyDataItem;
import com.volta.tests.TestBrokenLinks;
import io.restassured.path.json.JsonPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Utils {

    /**
     * Sends an HTTP GET request to the specified URL and returns the HTTP response code.
     *
     * The method opens an HttpURLConnection for the given link, follows all redirects
     * to reach the final destination URL, and retrieves the server response code.
     *
     * @param link unique URL to be checked
     * @return HTTP response code returned by the server;
     *         returns 500 if an exception occurs during the request
     */
    public static int getResponseCode(String link) {

        try {
            // System.out.println("trying = " + link);

            /* Object of HTTP Connection (with specified URL) */
            HttpURLConnection conn
                    = (HttpURLConnection) new URL(link).openConnection();

            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            );

            /* 'true' - if we need to follow all redirects and reach the final URL */
            conn.setInstanceFollowRedirects(true);

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            // conn.connect(); // connection is established, Java provides a socket, sends an HTTP request and waits for the server's response.

            return conn.getResponseCode(); // conn.connect() - will be established automatically!!!
            // System.out.println("Status Code: " + responseCode);

        } catch (Exception e) {
            return 500;
        }
    }

    /**
     * Outputs all extracted link entries for inspection and debugging purposes.
     *
     * The method iterates through the provided list of LinkCheckItem objects,
     * normalizes each raw link value, and prints id, type, normalized URL,
     * and original link to the console. It also prints the total number of
     * processed links.
     *
     * @param listAll list of LinkCheckItem elements containing extracted URLs
     */
    public static void itemLIstExtractor(List<LinkCheckItem> listAll) {

        int counter = 0;
        for (LinkCheckItem item : listAll) {
            int id = item.getId();
            String type = item.getType();
            String linkRaw = item.getLink();
            String linkNormalized = normalizeURL(linkRaw);
            System.out.println(id + " : " + type + " : " + linkNormalized + "  #link: " + linkRaw);
            counter++;
        }
        System.out.println("---------------------------------");
        System.out.println("Extracted total links = " + counter);
    }

    /**
     * Filters duplicate links within the same id and returns a collection of unique items.
     *
     * The method accepts a list of LinkCheckItem objects (id, type, link) and removes
     * duplicates so that the same id cannot contain multiple identical links.
     * Uniqueness is determined by the LinkCheckItem equality logic and preserved
     * using a LinkedHashSet to maintain insertion order.
     *
     * @param listAll list containing all extracted LinkCheckItem elements
     * @return Set of unique LinkCheckItem objects grouped by id and link uniqueness
     */
    public static Set<LinkCheckItem> itemListUniquesExtractor(List<LinkCheckItem> listAll) {

        Set<LinkCheckItem> set = new LinkedHashSet<>();
        set.addAll(listAll);

        for (LinkCheckItem each : set) {
            int id = each.getId();
            String type = each.getType();
            String linkRaw = each.getLink();
        }
        System.out.println("UniqueURLs List size = " + set.size());
        return set;
    }

    /**
     * Core validation mechanism responsible for detecting broken links
     * and writing validation results into an NDJSON report.
     *
     * The method accepts a complete list of extracted URL elements, where
     * the same id may contain duplicate links.
     *
     * Processing flow:
     * 1. Duplicate links are removed per id using itemListUniquesExtractor(),
     *    preserving the original insertion order.
     * 2. Each link is normalized via normalizeURL(), which filters unsupported
     *    formats and converts relative paths into absolute URLs.
     * 3. Links are evaluated according to validation rules.
     *
     * Validation rules:
     * - null result → link is skipped entirely.
     * - "__INVALID__" marker → link is treated as broken without HTTP request
     *   and written to NDJSON.
     * - blocked hosts → link is skipped and not validated.
     * - HTTP response code ≥ 400 → link is classified as broken and written to NDJSON.
     *
     * Only links that fail validation are added to the brokenLinks collection.
     *
     * @param list list of LinkCheckItem objects containing raw extracted URLs
     */
    public static void itemLIstValidator(List<LinkCheckItem> list) {

        Set<LinkCheckItem> setAll = itemListUniquesExtractor(list);

        List<LinkCheckItem> brokenLinks = new ArrayList<>();

        int count = 0;

        for (LinkCheckItem item : setAll) {

            int id = item.getId();
            String type = item.getType();
            String linkRaw = item.getLink();

            String linkNormalized = normalizeURL(linkRaw);
            System.out.println(id + " : " + type + " : " + linkNormalized + "  #link: " + linkRaw);
            ++count;

            if (linkNormalized == null) {
                continue;
            }
            // Writes into 'brokenLinks' directly
            if ("__INVALID__".equals(linkNormalized)) {
                brokenLinks.add(item);
                NdJsonWriter.write(item);
                continue;
            }
            // For blocked-host exceptions:
            if (BlockedHostsProvider.isBlocked(linkNormalized)) {
                continue;
            }
            // Writes into 'brokenLinks' after checking Condition400 by method getResponseCode(String link)
            int responseCode = getResponseCode(linkNormalized);
            if (responseCode >= 400) {
                brokenLinks.add(item);
                NdJsonWriter.write(item);
            }
        }
        System.out.println("_________________________________________________");
        System.out.println();
        System.out.println("Total Amount of ID    = " + TestBrokenLinks.getIdTotalCount());
        System.out.println("Retrieved ID Amount   = " + TestBrokenLinks.getIdRetrievedListSize());
        System.out.println("Amount of Raw URLs    = " + TestBrokenLinks.getRawLinkListSize());
        System.out.println("Amount of Unique URLs = " + count);
        System.out.println("Amount of Broken URLs = " + brokenLinks.size());
        System.out.println("_________________________________________________");
        System.out.println();
        System.out.println("Broken URLs List: " + brokenLinks);
    }

    /**
     * Normalizes a raw URL string according to predefined exclusion and validation rules.
     *
     * The method trims the input value, filters out unsupported links such as anchors,
     * javascript, and mailto references, and converts relative paths into absolute URLs
     * by prepending the domain name. If the URL does not match supported formats,
     * a special marker value is returned.
     *
     * @param raw raw URL value extracted from HTML attributes
     * @return normalized absolute URL, "__INVALID__" for unsupported formats,
     *         or null for excluded link types
     */
    public static String normalizeURL(String raw) {

        if (raw == null) {
            return null;
        }

        raw = raw.trim();

        if (raw.isEmpty()) {
            return "__INVALID__";
        }

        if (raw.startsWith("#")
                || raw.startsWith("javascript:")
                || raw.startsWith("mailto:")) {
            return null;
        }

        if (raw.startsWith("/")) {
            return ConfigurationReader.getProperty("url") + raw;
        }

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }

        return "__INVALID__";
    }

    /**
     * Extracts all link (href) and image (src) values from HTML content stored in JSON
     * and collects them into a unified list of LinkCheckItem objects.
     *
     * The method iterates through all JSON items, parses the verifyData field as HTML
     * using Jsoup.parse(), selects required elements via doc.select(), and maps each
     * extracted attribute to a LinkCheckItem associated with its corresponding id.
     *
     * @param jsPath JsonPath object containing response data with embedded HTML fragments
     * @return list of all extracted links and images wrapped as LinkCheckItem objects
     */
    public static List<LinkCheckItem> retrieveAllHrefAndSrcToList(JsonPath jsPath){

        // Retrieves all href & src on the List<LinkCheckItem> listAll:

        int listSize = jsPath.getList("data.id").size();
        // System.out.println("'ID' count in the response: " + listSize);

        List<LinkCheckItem> listAll = new ArrayList<>();

        // Iterates each id element to retrieve all 'href' and all 'scr' attributes value
        for (int i = 0; i < listSize; i++) {

            int id = jsPath.getInt("data[" + i + "].id");

            // Retrieves the field 'verifyData' as a String to use it with Jsoup.parse() to parse is as an HTML
            String verifyDataHTML = jsPath.getString("data[" + i + "].verifyData");
            // System.out.println("verifyData String = " + verifyDataHTML);

            /** For Attention - What Jsoup.parse() is for:
             * Jsoup.parse() - parses HTML document (verifyDataHTML - in our current case) - > returns a DOM structure = Document.
             * Document doc - allows us to use .select() for retrieving all specified tag with a specified attribute
             *    Examples:
             *             .select("div[class"]) -> returns all 'div' tags with 'class' attribute as a List<Element>
             *             .select("*[href]")    -> returns all 'any' tags with 'href' attribute
             *             .select("img[src]")   -> returns all 'img' tags with 'src' attribute
             *                - and then .attr("attribute") for retrieving the value from a specified attribute
             * JSON → jsonPath() → структура JSON
             * HTML → Jsoup.parse() → DOM (Document)
             *
             */
            Document doc = Jsoup.parse(verifyDataHTML);

            // Retrieves all 'href' values and put them on the List<LinkCheckItem> listAll
            Elements hrefSelect = doc.select("a[href]"); // Element in Jsoup - List<Elements> in or case it's a List of attributes = href
            // System.out.println("hrefSelect = " + hrefSelect);
            for (Element hrefEach : hrefSelect) {
                String href = hrefEach.attr("href");
                // link.attr("href") - retrieve the value (url) from attribute 'href'
                LinkCheckItem item = new LinkCheckItem();
                item.setId(id);
                item.setType("url");
                item.setLink(href);
                listAll.add(item);
            }

            // Retrieves all 'scr' values and put them on the List<LinkCheckItem> listAll
            Elements imgSelect = doc.select("img[src]");
            // System.out.println("imgSelect = " + imgSelect);
            for (Element imgEach : imgSelect) {
                String src = imgEach.attr("src");
                // img.attr("src") - retrieve the value (url) from attribute 'src'
                LinkCheckItem item = new LinkCheckItem();
                item.setId(id);
                item.setType("img");
                item.setLink(src);
                listAll.add(item);
            }
        }
        return listAll;
    }

    /**
     * Reads an NDJSON file and builds a payload structure grouped by id.
     *
     * The method reads the file line by line, deserializes each NDJSON record
     * into a LinkCheckItem object, and aggregates all entries by their id.
     * For each unique id, a single ExportItem is created containing a list
     * of VerifyDataItem elements (type + value), matching the required payload format.
     *
     * @param file_name name or path of the NDJSON file to be read
     * @return list of ExportItem objects structured as:
     *         id + verifyData list, ready for payload usage
     */
    public static List<ExportItem> readFromNdJson(String file_name){

        ObjectMapper objectMapper = new ObjectMapper(); // to read our file

        // Temporary indexed-Map to keep unique id:
        Map<Integer, ExportItem> payloadById = new HashMap<>();

        Path path = Paths.get(file_name); // "practicing_items.ndjson"

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;

            while ((line = reader.readLine()) != null){  // reader.readLine() - read line by line
                if (line.isBlank()) continue;
                // ✅ Deserialize each NDJSON line (String) into LinkCheckItem object
                LinkCheckItem item = objectMapper.readValue(line, LinkCheckItem.class);
                // System.out.println(item);

                int id = item.getId();
                String type = item.getType();
                String link = item.getLink();

                // Finds existent id or Create a new one with the container for its data:
                ExportItem payloadRaw = payloadById.get(id);
                if (payloadRaw == null){
                    payloadRaw = new ExportItem();
                    payloadRaw.setId(id);
                    payloadRaw.setVerifyData(new ArrayList<>());
                    payloadById.put(id, payloadRaw);
                }

                // Creates verifyData element:
                VerifyDataItem vd = new VerifyDataItem();
                vd.setType(type);
                vd.setValue(link);

                // Adds verifyData element to the container:
                payloadRaw.getVerifyData().add(vd);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(payloadById.values());
    }

    /**
     * Builds a JSON payload from an NDJSON file.
     *
     * The method reads and aggregates NDJSON records using readFromNdJson(),
     * then serializes the resulting list of ExportItem objects into a single
     * JSON string that matches the required payload structure.
     *
     * @param file_name name or path of the NDJSON file
     * @return JSON payload string generated from the NDJSON content
     * @throws JsonProcessingException if serialization fails
     */
    public static String payloadFromNdJson(String file_name) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(readFromNdJson(file_name));
    }

    /**
     * Counts the number of unique id entries stored in an NDJSON file.
     *
     * The method reads and aggregates NDJSON records via readFromNdJson()
     * and returns the total number of ExportItem objects, where each item
     * represents a unique id in the final payload structure.
     *
     * @param file_name name or path of the NDJSON file
     * @return number of unique id elements found in the file
     * @throws JsonProcessingException if NDJSON parsing fails
     */
    public static int countIdNumbers(String file_name) throws JsonProcessingException {
        List<ExportItem> exportItems = readFromNdJson(file_name);
        // System.out.println("exportItems List >>>> " + exportItems);
        return exportItems.size();
    }

    /**
     * Counts the number of non-empty lines in an NDJSON file.
     * A line is considered valid if it contains at least one non-whitespace character.
     * Empty lines (including those with spaces or tabs) are ignored.
     *
     * This method is intended for NDJSON reports where each non-empty line
     * represents a single exported JSON object.
     *
     * @param fileName path to the NDJSON file
     * @return number of non-empty lines (actual payload objects)
     * @throws IOException if the file cannot be read
     */
    public static long countLines(String fileName) throws IOException {
        long count = 0;
        try (BufferedReader reader = Files.newBufferedReader(Path.of(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }


}
