package com.sippy.wrapper.parent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sippy.wrapper.parent.database.DatabaseConnection;
import com.sippy.wrapper.parent.database.dao.TnbDao;
import com.sippy.wrapper.parent.request.JavaTestRequest;
import com.sippy.wrapper.parent.response.JavaTestResponse;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class WrappedMethods {

  private static final Logger LOGGER = LoggerFactory.getLogger(WrappedMethods.class);

  @EJB DatabaseConnection databaseConnection;

  @RpcMethod(name = "javaTest", description = "Check if everything works :)")
  public Map<String, Object> javaTest(JavaTestRequest request) {
    JavaTestResponse response = new JavaTestResponse();

    int count = databaseConnection.getAllTnbs().size();

    LOGGER.info("the count is: " + count);

    response.setId(request.getId());
    String tempFeeling = request.isTemperatureOver20Degree() ? "warm" : "cold";
    response.setOutput(
        String.format(
            "%s has a rather %s day. And he has %d tnbs", request.getName(), tempFeeling, count));

    Map<String, Object> jsonResponse = new HashMap<>();
    jsonResponse.put("faultCode", "200");
    jsonResponse.put("faultString", "Method success");
    jsonResponse.put("something", response);

    return jsonResponse;
  }

  @RpcMethod(name = "getTnbList", description = "Get Tnb List")
  public String getTnbList(String jsonResponse) throws JsonProcessingException {

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(jsonResponse);
    JsonNode params = jsonNode.get("params");

    LOGGER.info("Fetching TNB list from the database");
    List<TnbDao> allTnbs = databaseConnection.getAllTnbs();

    int numberParam = params.get("number").asInt();

    Optional<TnbDao> tnb = allTnbs.stream().filter(tnbDao -> tnbDao.getName().equals(String.valueOf(numberParam))).findFirst();

    TnbDao newTnd = new TnbDao();
    newTnd.setTnb("D001");
    newTnd.setName("Deutsche Telekom");
    newTnd.setIsTnb(tnb.isPresent() && tnb.get().getTnb().equals("D001"));
    List<TnbDao> tnbs = new ArrayList<>();

    Pattern pattern = Pattern.compile("(D146|D218|D248)");
    List<TnbDao> tnbdListFiltered;

    tnbdListFiltered = allTnbs.stream().filter(tnbDao -> !pattern.matcher(tnbDao.getTnb()).find()).collect(Collectors.toList());
    tnbdListFiltered.forEach(tnbDao -> tnbDao.setIsTnb(tnb.isPresent() && tnb.get().getTnb().equals(tnbDao.getTnb())));
    tnbs.add(newTnd);
    tnbs.addAll(tnbdListFiltered);

    List<TnbDao> tndListSorted = tnbs.stream()
            .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
            .collect(Collectors.toList());
    String tnbJson = objectMapper.writeValueAsString(tndListSorted);


    Map<String, Object> resultMap = Map.of("faultcode", "200", "faultString", "Method success", "tnbs", tndListSorted);
    return objectMapper.writeValueAsString(resultMap);
  }

}
