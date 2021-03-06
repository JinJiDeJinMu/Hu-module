package com.hjb.es;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EsServiceImpl implements EsService{

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public CreateIndexResponse createIndex(String index, String mapping, String setting) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
        if(!StringUtils.isEmpty(mapping)){
            createIndexRequest.mapping(mapping, XContentType.JSON);
        }
        if(!StringUtils.isEmpty(setting)){
            createIndexRequest.settings(setting,XContentType.JSON);
        }
        CreateIndexResponse createIndexResponse = null;
        try {
            createIndexResponse = restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            log.info("create index={}, response ={}",index, createIndexResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createIndexResponse;
    }

    @Override
    public Boolean deletedIndex(String index) {
        DeleteIndexRequest deleteindexRequest = new DeleteIndexRequest(index);
        deleteindexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
        Boolean result = false;
        try {
            AcknowledgedResponse delete = restHighLevelClient.indices().delete(deleteindexRequest,RequestOptions.DEFAULT);
            result = delete.isAcknowledged();
            log.info("delete index={}, reponse={}",index, delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public IndexResponse insertIndex(String index, String type, String id, Object object) {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(index);
        if(!StringUtils.isEmpty(id)){
            indexRequest.id(id);
        }
        if(!StringUtils.isEmpty(type)){
            indexRequest.type(type);
        }
        indexRequest.source(JSON.parseObject(JSON.toJSONString(object), Map.class));

        IndexResponse indexResponse = null;
        try {
            indexResponse = restHighLevelClient.index(indexRequest,RequestOptions.DEFAULT);
            log.info("insert index={}, response={}", index, indexResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return indexResponse;
    }

    @Override
    public BulkResponse bulkIndex(String index, List<Object> list) {
        BulkRequest request = new BulkRequest();
        list.forEach(e->{
            IndexRequest indexRequest = new IndexRequest(index);
            indexRequest.source(JSON.parseObject(JSON.toJSONString(e), Map.class));
            request.add(indexRequest);
        });
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = restHighLevelClient.bulk(request,RequestOptions.DEFAULT);
            log.info("bulk insert index={}, params={}, response={}",index, list, bulkResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bulkResponse;
    }

    @Override
    public SearchResponse search(String index,String source ,String keyword) {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchQuery(source,keyword));



        //queryStringQuery全文查询
        sourceBuilder.query(QueryBuilders.queryStringQuery("xx"));

        //短语匹配 ,和match查询类似，match_phrase查询首先解析查询字符串来产生一个词条列表。
        // 然后会搜索所有的词条，但只保留包含了所有搜索词条的文档，并且词条的位置要邻接。
        // 一个针对短语“中华共和国”的查询不会匹配“中华人民共和国”，因为没有含有邻接在一起的“中华”和“共和国”词条。
        sourceBuilder.query(QueryBuilders.matchPhraseQuery("xx","yy"));

        //multi_match多个字段匹配某字符串
        sourceBuilder.query(QueryBuilders.multiMatchQuery("xx","yy","zz"));

        //完全包含查询,之前的查询中，当我们输入“我天”时，ES会把分词后所有包含“我”和“天”的都查询出来
        //如果我们希望必须是包含了两个字的才能被查询出来，那么我们就需要设置一下Operator。and或者or
        sourceBuilder.query(QueryBuilders.matchQuery("xx","yy").operator(Operator.AND));

        sourceBuilder.query(boolQueryBuilder);

        //高亮查询
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(new HighlightBuilder.Field("address"));
        highlightBuilder.preTags("<span style=\"color:red;size:50px\">");   //高亮设置
        highlightBuilder.postTags("</span>");
        highlightBuilder.fragmentSize(50);
        highlightBuilder.numOfFragments(0);//从第一个分片开始获取高亮片段
        sourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(sourceBuilder);
        searchRequest.indices(index);

        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest,RequestOptions.DEFAULT);
            log.info("search index={}, params={}, response={}",index, keyword, searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResponse;
    }
}
