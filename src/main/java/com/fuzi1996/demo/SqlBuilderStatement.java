package com.fuzi1996.demo;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLIncludeTransformer;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.List;

public class SqlBuilderStatement {
    private static final Log log = LogFactory.getLog(SqlBuilderStatement.class);

    private static final String SCRIPT_PREFIX = "<script>";
    private static final String SCRIPT_SUFFIX = "</script>";

    private Configuration configuration;
    private LanguageDriver languageDriver;

    public SqlBuilderStatement(Configuration configuration) {
        this.configuration = configuration;
        this.languageDriver = configuration.getDefaultScriptingLanguageInstance();
    }

    /**
     * 生成缓存map的缓存key
     * @param sql
     * @param parameterType
     * @param resultType
     * @param sqlCommandType
     * @return
     */
    private String getCacheKey(String sql, Class<?> parameterType,
                               Class<?> resultType, SqlCommandType sqlCommandType) {
        return sqlCommandType.toString() + "." + (resultType + sql + parameterType)
                .hashCode();
    }

    /**
     * 判断是否已经缓存
     * @param cacheKey
     * @return
     */
    private boolean hasMappedStatement(String cacheKey) {
        return this.configuration.hasStatement(cacheKey, false);
    }

    /**
     * 当没有缓存MappedStatement时，添加MappedStatement缓存
     * @param cacheKey
     * @param sqlSource
     * @param resultType
     */
    private MappedStatement cacheSelectMappedStatement(String cacheKey,
                                                       SqlSource sqlSource,
                                                       final Class<?> resultType) {
        List<ResultMap> resultMapList = new ArrayList<>();
        resultMapList.add((new ResultMap.Builder(this.configuration,
                "defaultResultMap", resultType, new ArrayList(0))).build());
        MappedStatement mappedStatement = (new MappedStatement.Builder(
                this.configuration,
                cacheKey,
                sqlSource,
                SqlCommandType.SELECT)).resultMaps(resultMapList).build();
        this.configuration.addMappedStatement(mappedStatement);
        return mappedStatement;
    }

    /**
     * 获取缓存key,如果没有缓存则会添加MappedStatement缓存
     * @param sql
     * @param parameterType
     * @param resultType
     * @return
     */
    public String getCacheKeyWithStore(String sql, Class<?> parameterType,
                                        Class<?> resultType) {
        sql = this.appendScriptTag(sql);
        String cacheKey = this
                .getCacheKey(sql, parameterType, resultType, SqlCommandType.SELECT);
        if (!this.hasMappedStatement(cacheKey)) {
            if (log.isDebugEnabled()) {
                log.debug("cacheKey [" + cacheKey + "] for sql ["+sql+"] hit cache ");
            }
            MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration,"");
            XPathParser xPathParser = new XPathParser(sql);
            XNode context = xPathParser.evalNode("//script");
            XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
            includeParser.applyIncludes(context.getNode());
            SqlSource sqlSource = this.languageDriver
                    .createSqlSource(this.configuration, context, parameterType);
            this.cacheSelectMappedStatement(cacheKey, sqlSource, resultType);
        }else{
            if (log.isDebugEnabled()) {
                log.debug("cacheKey [" + cacheKey + "] for sql ["+sql+"] not hit cache ");
            }
        }
        return cacheKey;
    }

    private String appendScriptTag(String sql){
        StringBuffer sqlBuff = new StringBuffer();
        if(!sql.startsWith(SCRIPT_PREFIX)){
            sqlBuff.append(SCRIPT_PREFIX);
        }
        sqlBuff.append(sql);
        if(!sql.endsWith(SCRIPT_SUFFIX)){
            sqlBuff.append(SCRIPT_SUFFIX);
        }
        return sqlBuff.toString();
    }

}
