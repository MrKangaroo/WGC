package com.wgc.common.utils;


import com.wgc.common.config.Constant;
import com.wgc.common.domain.ColumnDO;
import com.wgc.common.domain.TableDO;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 */
public class GenUtils {


    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("templates/generator/Domain.java.vm");
        templates.add("templates/generator/Dao.java.vm");
        templates.add("templates/generator/Service.java.vm");
        templates.add("templates/generator/ServiceImpl.java.vm");
        templates.add("templates/generator/Controller.java.vm");

//        templates.add("templates/generator/Mapper.xml.vm");
//        templates.add("templates/generator/list.html.vm");
//        templates.add("templates/generator/add.html.vm");
//        templates.add("templates/generator/edit.html.vm");
//        templates.add("templates/generator/list.js.vm");
//        templates.add("templates/generator/add.js.vm");
//        templates.add("templates/generator/edit.js.vm");

        templates.add("templates/generator/RpcService.java.vm");
        templates.add("templates/generator/RpcServiceImpl.java.vm");
        templates.add("templates/generator/SystemRequest.java.vm");
        templates.add("templates/generator/SystemResponse.java.vm");

        templates.add("templates/generator/Query.java.vm");
        templates.add("templates/generator/Dto.java.vm");
        templates.add("templates/generator/Dao.xml.vm");
        templates.add("templates/generator/Converter.xml.vm");
        templates.add("templates/generator/FetchListRequest.java.vm");
        templates.add("templates/generator/FetchRequest.java.vm");
        templates.add("templates/generator/DeleteRequest.java.vm");
        templates.add("templates/generator/CreateOrModifyRequest.java.vm");

        return templates;
    }

    /**
     * 生成代码
     */


    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        //表信息
        TableDO tableDO = new TableDO();
        tableDO.setTableName(table.get("tableName"));
        tableDO.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableDO.getTableName(), config.getString("tablePrefix"), config.getString("autoRemovePre"));
        tableDO.setClassName(className);
        tableDO.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnDO> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnDO columnDO = new ColumnDO();
            columnDO.setColumnName(column.get("columnName"));
            columnDO.setDataType(column.get("dataType"));
            columnDO.setComments(column.get("columnComment"));
            columnDO.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnDO.getColumnName());
            columnDO.setAttrName(attrName);
            columnDO.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnDO.getDataType(), "unknowType");
            columnDO.setAttrType(attrType);

            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableDO.getPk() == null) {
                tableDO.setPk(columnDO);
            }

            columsList.add(columnDO);
        }
        tableDO.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableDO.getPk() == null) {
            tableDO.setPk(tableDO.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        //封装模板数据
        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableDO.getTableName());
        map.put("comments", tableDO.getComments());
        map.put("pk", tableDO.getPk());
        map.put("className", tableDO.getClassName());
        map.put("classname", tableDO.getClassname());
        map.put("pathName", config.getString("package").substring(config.getString("package").lastIndexOf(".") + 1));
        map.put("columns", tableDO.getColumns());
        map.put("package", config.getString("package"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableDO.getClassname(), tableDO.getClassName(), config.getString("package").substring(config.getString("package").lastIndexOf(".") + 1))));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new BDException("渲染模板失败，表名：" + tableDO.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix, String autoRemovePre) {
        if (Constant.AUTO_REOMVE_PRE.equals(autoRemovePre)) {
            tableName = tableName.substring(tableName.indexOf("_") + 1);
        }
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
        }

        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new BDException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String classname, String className, String packageName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }

        if (template.contains("Domain.java.vm")) {
            return packagePath + "domain" + File.separator + className + "DO.java";
        }

        if (template.contains("Dao.java.vm")) {
            return packagePath + "dao" + File.separator + className + "Dao.java";
        }
        if (template.contains("RpcService.java.vm")) {
            return packagePath + "service" + File.separator + "I" + className + "RpcService.java";
        }

        if (template.contains("RpcServiceImpl.java.vm")) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "RpcServiceImpl.java";
        }

        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + "I" + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm")) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }
        if (template.contains("SystemRequest.java.vm")) {
            return packagePath + "domain" + File.separator + className + "SystemRequest.java";
        }

        if (template.contains("SystemResponse.java.vm")) {
            return packagePath + "domain" + File.separator + className + "SystemResponse.java";
        }

        if (template.contains("Controller.java.vm")) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains("Query.java.vm")) {
            return packagePath + "query"+ File.separator + className + File.separator + className + "Query.java";
        }
        if (template.contains("Dto.java.vm")) {
            return packagePath + "dto"+ File.separator + className + File.separator + className + "Dto.java";
        }
        if (template.contains("Dao.xml.vm")) {
            return packagePath + "Dao"+ File.separator + className + File.separator + className + "Dao.java";
        }
        if (template.contains("Converter.java.vm")) {
            return packagePath + "converter" + File.separator + className+ File.separator + className + "Converter.java";
        }
        if (template.contains("FetchListRequest.java.vm")) {
            return packagePath + "request"+ File.separator + className + File.separator + className + "FetchListRequest.java";
        }
        if (template.contains("FetchRequest.java.vm")) {
            return packagePath + "request" + File.separator + className+ File.separator + className + "FetchRequest.java";
        }
        if (template.contains("DeleteRequest.java.vm")) {
            return packagePath + "request"+ File.separator + className + File.separator + className + "DeleteRequest.java";
        }
        if (template.contains("CreateOrModifyRequest.java.vm")) {
            return packagePath + "request" + File.separator + className + File.separator + className + "CreateOrModifyRequest.java";
        }

//        if (template.contains("Mapper.xml.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + packageName + File.separator + className + "Mapper.xml";
//        }
//        if (template.contains("list.html.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
//                    + packageName + File.separator + classname + File.separator + classname + ".html";
//            //				+ "modules" + File.separator + "generator" + File.separator + className.toLowerCase() + ".html";
//        }
//        if (template.contains("add.html.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
//                    + packageName + File.separator + classname + File.separator + "add.html";
//        }
//        if (template.contains("edit.html.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
//                    + packageName + File.separator + classname + File.separator + "edit.html";
//        }
//
//        if (template.contains("list.js.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "static" + File.separator + "js" + File.separator
//                    + "appjs" + File.separator + packageName + File.separator + classname + File.separator + classname + ".js";
//        }
//        if (template.contains("add.js.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "static" + File.separator + "js" + File.separator
//                    + "appjs" + File.separator + packageName + File.separator + classname + File.separator + "add.js";
//        }
//        if (template.contains("edit.js.vm")) {
//            return "main" + File.separator + "resources" + File.separator + "static" + File.separator + "js" + File.separator
//                    + "appjs" + File.separator + packageName + File.separator + classname + File.separator + "edit.js";
//        }
        return null;
    }
}
