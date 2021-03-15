package com.i2dsp.i2dspcloudv2_new.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.i2dsp.i2dspcloudv2_new.domain.Device;

import com.i2dsp.i2dspcloudv2_new.domain.deviceModel.Device_state;
import com.i2dsp.i2dspcloudv2_new.service.DeviceService;
import com.i2dsp.i2dspcloudv2_new.utils.I2dspException;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @Author: 梁其定
 * @DateTime: 2020/4/27 0027 9:36
 * @Description: TODO
 */
@Service
@CacheConfig(cacheNames = "device")
public class DeviceServiceImpl implements DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);
    //前端查询条件事件类型
    private final Device_state emg = new Device_state().setFunction_tag("MS103Emg").setValue(1);
    private final Device_state cancelemg = new Device_state().setFunction_tag("MS103Emg").setValue(0);
    private final Device_state lowvol = new Device_state().setFunction_tag("SD103Battery").setValue(0);
    private final Device_state cancellowvol = new Device_state().setFunction_tag("SD103Battery").setValue(100);
    private final Device_state offline = new Device_state().setFunction_tag("MS103Emg").setValue(1);
    private final Device_state online = new Device_state().setFunction_tag("MS103Emg").setValue(1);

    private static final String[] product = {
            "LT-102",
            "LT-103",
            "LT-103EX",
            "LT-102M",
            "LT-103NET",
            "LT-102S",
            "LT-10",
            "HS-10",
            "AS-10",
            "AS-10",
            "IO-10",
            "SD103",
            "TD-500",
            "HAS-100",
            "HS-100",
            "AS-100",
            "FS-810"
    };
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private Device_addressServiceImpl device_addressService;
    @Autowired
    private User_deviceServiceImpl user_deviceService;

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 设备查询
     * @Date 15:20 2020/4/28 0028
     * @Param
     **/
    @Override
    public PageImpl<Document> getDevice(JSONArray jsonArray, String user_id, Integer pageNum, Integer pageSize, String type, String deviceId) throws I2dspException {
        boolean subdev = false;
        String device_id = "";

        Sort sort = Sort.by(Sort.Direction.DESC, "_id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        ArrayList<Criteria> criteriaArrayList = new ArrayList<>();
        ArrayList<Criteria> criteriaArrayListOr = new ArrayList<>();
        if (user_id == null) {
            return null;
        }
        if (deviceId != null) {
            Pattern pattern = Pattern.compile("^.*" + deviceId + ".*$", Pattern.CASE_INSENSITIVE);
            criteriaArrayList.add(Criteria.where("_id").regex(pattern));
        }
        List<String> strings = userServiceImpl.countDeviceIdByUserId(user_id);// todo 统计该用户所有设备id
        logger.info("当前查询用户id:" + user_id);
        criteriaArrayList.add(Criteria.where("_id").in(strings));

        if (jsonArray.size() > 0 && !jsonArray.getJSONObject(0).isEmpty()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
                // todo 判断是否组合查询
                if (jsonObject.entrySet().size() == 1 && !jsonObject.containsKey("device_id")) {
                    Map.Entry<String, Object> next = iterator.next();
                    if (next.getKey().contains("address.")) {
                        criteriaArrayList.add(Criteria.where("device_address." + next.getKey()).is(next.getValue()));
                    } else {
                        criteriaArrayList.add(Criteria.where(next.getKey()).is(next.getValue()));
                    }
                } else if (jsonObject.containsKey("device_id")) {
                    device_id = jsonObject.getString("device_id");
                    if (jsonObject.containsKey("subdev") && ("true").equals(jsonObject.getString("subdev"))) {
                        subdev = true;
                        List<String> ids = countAllSumList(device_id);
                        logger.info("当前查询设备id:" + device_id + "，包含设备：" + ids);
                        ids.add(device_id);
                        criteriaArrayList.add(Criteria.where("parent_devid").in(ids));
                    } else {
                        criteriaArrayList.add(Criteria.where("_id").is(device_id));
                    }
                } else {
                    if (!ToMatch(criteriaArrayList, iterator)) {
                        return null;
                    }
                }
            }
        }
        List<Document> devicesMain = find_device(criteriaArrayList);
        //        todo 去除字段 device_topiclist
        ArrayList<Document> documents = new ArrayList<Document>();
        if (subdev) {
            ArrayList<Criteria> criteriaArrayListTwo = new ArrayList<>();
            criteriaArrayListTwo.add(Criteria.where("_id").is(device_id));
            List<Document> devicesSecond = find_device(criteriaArrayListTwo);
            if (type.equals("app")) {
                devicesSecond.forEach(document -> document.remove("device_topiclist"));
            }
            documents.addAll(devicesSecond);
        }

        if (type.equals("app")) {
            devicesMain.forEach(document -> document.remove("device_topiclist"));
        }
        documents.addAll(devicesMain);
        return new PageImpl<Document>(documents.stream().skip((pageNum - 1) * pageSize).limit(pageSize).collect(Collectors.toList()), pageable, documents.size());
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 统计所有子id  不包含它本身
     * @Date 8:14 2020/4/28 0028
     * @Param
     **/
    @Cacheable(key = "#root.methodName+'-'+#p0")
    @Override
    public List<String> countAllSumList(String id) {
        List<String> old_IDS = new ArrayList<String>();
        List<String> new_IDS = new ArrayList<String>();
        old_IDS.add(id);
        while (old_IDS.size() > 0) {
            List<Device> devices = mongoTemplate.find(Query.query(Criteria.where("parent_devid").in(old_IDS)), Device.class);
            old_IDS = devices.stream().map(Device::get_id).collect(Collectors.toList());
            new_IDS.addAll(old_IDS);
        }
        return new_IDS;
    }

    @Override
    public Page<Device> getAllDevice(String user_id, Integer pageNum, Integer pageSize) {
        System.out.println("pageNum=" + pageNum);
        Sort sort = Sort.by(Sort.Direction.DESC, "_id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        List<String> device_id = userServiceImpl.countDeviceIdByUserId(user_id);
        List<Device> devices = mongoTemplate.find(Query.query(Criteria.where("_id").in(device_id)).skip((pageNum - 1) * pageSize).limit(pageSize), Device.class);
        long id = mongoTemplate.count(Query.query(Criteria.where("_id").in(device_id)), Device.class);
        return new PageImpl<Device>(devices, pageable, id);
    }

    /**
     * @return
     * @Author QiDing
     * @Description //TODO 统计该用户的子用户，未拥有的设备
     * @Date 15:22 2020/6/18 0018
     * @Param
     **/
    @Override
    public Page<Device> getAllDeviceNotIn(String user_id, List<String> deviceIds, String content, Integer pageNum, Integer pageSize) {
        if (content == null) {
            content = "";
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "_id");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        List<String> deviceId = user_deviceService.countDeviceId(user_id);

        Pattern pattern = Pattern.compile("^.*" + content + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = new Criteria().andOperator(Criteria.where("_id").not().in(deviceIds), Criteria.where("_id").in(deviceId));
        Criteria criteria2 = new Criteria().orOperator(
                Criteria.where("_id").regex(pattern)
        );
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        List<Device> devices = mongoTemplate.find(Query.query(criteria).skip((pageNum - 1) * pageSize).limit(pageSize), Device.class);
        System.out.println(devices);
        long count = mongoTemplate.count(Query.query(criteria), Device.class);
        System.out.println(count);
        return new PageImpl<Device>(devices, pageable, count);
    }

    /**
     * @return
     * @Author QiDing
     * @Description //TODO 设备类型占比
     * @Date 14:29 2020/7/14 0014
     * @Param
     **/
    @Override
    public JSONArray countDeviceByType(List<String> deviceIds) {
        ArrayList<AggregationOperation> operations = new ArrayList<>();
        Criteria id = Criteria.where("_id").in(deviceIds);
        GroupOperation as = Aggregation.group("productkey").count().as("value");
        operations.add(Aggregation.match(id));
        operations.add(as);
        Aggregation aggregation = Aggregation.newAggregation(operations);
        // 执行查询
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "device", Document.class);
        List<NameValue> nameValues = JSONArray.parseArray(JSON.toJSONString(results.getMappedResults()).replaceAll("_id", "name"), NameValue.class);
//        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(results.getMappedResults()));
        JSONArray jsonArray = new JSONArray();

        HashMap<String, NameValue> nameValueHashMap = new HashMap<>();
        nameValues.forEach(nameValue -> nameValueHashMap.put(nameValue.name, nameValue));
        for (String pu : product
        ) {
//            String replace = pu.replace("-", "");
//            if (nameValueHashMap.containsKey(replace)) {
//                NameValue nameValue = nameValueHashMap.get(replace);
//                jsonArray.add(nameValue.setName(pu));
//                nameValueHashMap.remove(replace);
//            } else {
//                jsonArray.add(new NameValue().setName(pu));
//            }
//
            if (nameValueHashMap.containsKey(pu)) {
                NameValue nameValue = nameValueHashMap.get(pu);
                jsonArray.add(nameValue.setName(pu));
                nameValueHashMap.remove(pu);
            } else {
                jsonArray.add(new NameValue().setName(pu));
            }
        }
        int count = 0;
        for (Map.Entry<String, NameValue> next : nameValueHashMap.entrySet()) {
            count += next.getValue().value;
        }
        jsonArray.add(new NameValue().setName("其它").setValue(count));
        return jsonArray;
    }

    @Override
    public Device getParentDev(String device_id) {
        Device device = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(device_id)), Device.class);
        try {
            if (device != null) {
                String parent_id = device.getParent_devid();
                while (!parent_id.equals("cloud")) {
                    device = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(parent_id)), Device.class);
                    parent_id = device.getParent_devid();
                }
                return device;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public long countDevState(List<String> deviceIds, JSONArray jsonArray) {
        ArrayList<Criteria> devWhereList = getDevWhereList(deviceIds, jsonArray);
        Criteria[] criteriaS = new Criteria[devWhereList.size()];
        devWhereList.toArray(criteriaS);
        Criteria criteria = new Criteria().andOperator(criteriaS);
        return mongoTemplate.count(Query.query(criteria),Device.class);
    }


    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 设备数量统计（1）
     * @Date 11:01 2020/5/4 0004
     * @Param
     **/
    @Override
    public JSONArray getDevData(String user_id, JSONArray jsonArray, Integer pageNum, Integer pageSize, boolean b) {
        if (user_id == null) {
            return null;
        }
        JSONArray J = new JSONArray();
//        todo 获取用户所拥有的 device_id
        List<String> user_role = userServiceImpl.countDeviceIdByUserId(user_id);
        if (b) {
            //        todo 通过地址获取projectID
            ArrayList<Criteria> criteria_A = new ArrayList<>();
            for (int j = 0; j < jsonArray.size(); j++) {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> next = iterator.next();
                    if (next.getKey().contains("address.province")) {
                        criteria_A.add(Criteria.where(next.getKey()).is(next.getValue()));
                    }
                }
            }
            List<String> projectByAddress = device_addressService.findProjectByAddress(criteria_A);
//            todo 通过projectID 反查询设备id
            List<String> strings = device_addressService.countDeviceIdINProjectID(projectByAddress);
            List<String> collect = strings.stream().filter(user_role::contains).collect(Collectors.toList());
            ArrayList<Criteria> device_criteria = getDevWhereList(collect, jsonArray);
            AggregationResults<Document> documents = aggregationGroup(device_criteria);
            JSONObject jsonObject = toJson(documents);
            if (!jsonObject.isEmpty()) {
                J.add(jsonObject);
                jsonObject.put("address", getAddress(jsonArray));
            }
            return J;
        }

//       todo 获取projiect
        List<String> strings = device_addressService.countProjectId(user_role);
        for (String ProjectId : strings
        ) {
            List<String> strings1 = device_addressService.countDeviceIdByProjectID(ProjectId);
            List<String> collect = strings1.stream().filter(user_role::contains).collect(Collectors.toList());
            ArrayList<Criteria> device_criteria = getDevWhereList(collect, jsonArray);
            AggregationResults<Document> documents = aggregationGroup(device_criteria);
            JSONObject jsonObject = toJson(documents);
            if (!jsonObject.isEmpty()) {
                J.add(jsonObject);
                jsonObject.put("projectid", ProjectId);
            }
        }
        return J;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO OR 条件设备数量统计
     * @Date 8:48 2020/5/8 0008
     * @Param
     **/
    @Override
    public JSONArray getDevCount(String projectId, JSONArray jsonArray) {
        List<String> deviceIds = device_addressService.countDeviceIdByProjectID(projectId);
        ArrayList<Criteria> devWhereList = getDevWhereList(deviceIds, jsonArray);
        AggregationResults<Document> documents = aggregationGroupCount(devWhereList);
        return null;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 获取地址
     * @Date 16:26 2020/5/4 0004
     * @Param
     **/
    private JSONObject getAddress(JSONArray jsonArray) {
        JSONObject address = new JSONObject();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> next = iterator.next();
                if (next.getKey().contains("address.")) {
                    address.put(next.getKey(), next.getValue());
                }
            }

        }
        return address;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO json处理
     * @Date 14:59 2020/5/4 0004
     * @Param
     **/
    private JSONObject toJson(AggregationResults<Document> documents) {
        List<Document> mappedResults = documents.getMappedResults();
        if (mappedResults.size() <= 0) {
            return new JSONObject();
        }
        JSONObject data = new JSONObject();
        data.put("devcnt", mappedResults.size());
        JSONArray state = new JSONArray();
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(mappedResults));
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            JSONObject o = jsonArray.getJSONObject(i);

            JSONArray device_state = o.getJSONArray("device_state");
            for (int j = 0; j < device_state.size(); j++) {
                JSONObject jsonObject1 = device_state.getJSONObject(j);
                Object value = jsonObject1.get("value");
                if (value != null && !("").equals(value)) {
                    jsonObject.put(jsonObject1.getString("function_tag"), value);
                } else {
                    jsonObject.put(jsonObject1.getString("function_tag"), 0);
                }
            }
            jsonObject.put("devtype", o.getString("productkey"));
//            device_state.put("devtype",o.getString("productkey"));
            state.add(jsonObject);
            data.put("state", state);
        }

        return data;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 设备数量统计（2）——条件过滤
     * @Date 16:38 2020/4/28 0028
     * @Param
     **/
    private ArrayList<Criteria> getDevWhereList(List<String> deviceIds, JSONArray jsonArray) {
        ArrayList<Criteria> device_criteria = new ArrayList<>();
        device_criteria.add(Criteria.where("_id").in(deviceIds));
        if (jsonArray.size() > 0) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
                if (jsonObject.entrySet().size() == 1) {
                    Map.Entry<String, Object> next = iterator.next();
                    if (next.getKey().contains("address.")) {
                        device_criteria.add(Criteria.where("device_address." + next.getKey()).is(next.getValue()));
                    } else {
                        device_criteria.add(Criteria.where(next.getKey()).is(next.getValue()));
                    }
                } else {
                    //todo 精确查询数组
                    if (jsonObject.containsKey("address.province")) {
                        while (iterator.hasNext()) {
                            Map.Entry<String, Object> next = iterator.next();
                            device_criteria.add(Criteria.where("address." + next.getKey()).is(next.getValue()));
                        }
                    } else {
                        ToMatch(device_criteria, iterator);
                    }
                }
            }
        }
        return device_criteria;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 聚合查询 （3） GroupByProjectId   group:"address.detailaddress.mapid.projectid"  address:address.province
     * @Date 10:37 2020/5/4 0004
     * @Param
     **/
    public AggregationResults<Document> aggregationGroup(List<Criteria> device_criteria) {
        ArrayList<AggregationOperation> operations = new ArrayList<>();
        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("device_address")
                .localField("_id")
                .foreignField("_id")
                .as("device_address");
        operations.add(lookupOperation);
        if (device_criteria.size() != 0) {
            for (Criteria C : device_criteria
            ) {
                operations.add(Aggregation.match(C));
            }
        }
        operations.add(Aggregation.group("_id")
                .first("device_state").as("device_state")
                .first("productkey").as("productkey")
        );
        Aggregation aggregation = Aggregation.newAggregation(operations);
        // 执行查询
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "device", Document.class);
        return results;
    }


    /**
     * @Author 梁其定
     * @Description //TODO 使用match 对数据内数据精确查询  设备数量统计(3)
     * @Date 14:59 2020/4/28 0028
     * @Param criteriaArrayList：查询集合    iterator 查询条件
     **/
    private boolean ToMatch(ArrayList<Criteria> device_criteria, Iterator<Map.Entry<String, Object>> iterator) {
        String key1 = "";
        ArrayList<Criteria> match = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                Map.Entry<String, Object> next = iterator.next();
                String key = next.getKey();
                int i1 = key.lastIndexOf(".");
                key1 = key.substring(0, i1);
                String key2 = key.substring(i1 + 1);
                match.add(Criteria.where(key2).is(next.getValue()));
            }
        } catch (Exception e) {
            return false;
        }
        Criteria[] arr_criteria = new Criteria[match.size()];
        match.toArray(arr_criteria);
        Criteria match_criteria = new Criteria().andOperator(arr_criteria);
        device_criteria.add(new Criteria(key1).elemMatch(match_criteria));
        return true;
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 聚合查询
     * @Date 16:03 2020/4/27 0027
     * @Param
     **/
    @Override
    public List<Document> find_device(List<Criteria> criteria) {
        List<AggregationOperation> operations = new ArrayList<>();
        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("device_address")
                .localField("_id")
                .foreignField("_id")
                .as("device_address");
        // 把条件封装成List
        // 拼装具体查询信息
        Criteria docCri = Criteria.where("device").not().size(0);
        operations.add(lookupOperation);
        AggregationOperation match = Aggregation.match(docCri);
        operations.add(match);
        for (Criteria C : criteria
        ) {
            operations.add(Aggregation.match(C));
        }
        // 构建 Aggregation
        Aggregation aggregation = Aggregation.newAggregation(operations);
        // 执行查询
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "device", Document.class);
        return results.getMappedResults();
    }

    /**
     * @return
     * @Author 梁其定
     * @Description //TODO 聚合查询 统计数量
     * @Date 8:58 2020/5/8 0008
     * @Param
     **/
    private AggregationResults<Document> aggregationGroupCount(List<Criteria> device_criteria) {
        ArrayList<AggregationOperation> operations = new ArrayList<>();
        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("device_address")
                .localField("_id")
                .foreignField("_id")
                .as("device_address");
        operations.add(lookupOperation);
        if (device_criteria.size() != 0) {
            for (Criteria C : device_criteria
            ) {
                operations.add(Aggregation.match(C));
            }
        }
        operations.add(Aggregation.group("address.detailaddress.mapid.projectid")
                .count()
                .as("devcnt")
                .first("address.detailaddress.mapid.projectid").as("projectid")
                .addToSet("device_state").as("state")
        );
//         构建 Aggregation
        Aggregation aggregation = Aggregation.newAggregation(operations);
        // 执行查询
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "device", Document.class);
        return results;
    }

    @Data
    @Accessors(chain = true)
    static class NameValue {
        private String name;
        private Integer value = 0;
    }
}
