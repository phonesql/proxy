package com.phonesql.proxy.postgres.queryhandler;

import java.util.List;

public class PgGetKeywords implements QueryHandler {

    final String PREFIX = "select string_agg(word, ',') from pg_catalog.pg_get_keywords";

    final String WORDS =
            "abort,absent,access,aggregate,also,analyse,analyze,attach,backward,bit,cache,checkpoint,class,cluster,columns,comment,comments,compression,concurrently,conditional,configuration,conflict,connection,content,conversion,copy,cost,csv,current_catalog,current_schema,database,delimiter,delimiters,depends,detach,dictionary,disable,discard,do,document,empty,enable,encoding,encrypted,enum,error,event,exclusive,explain,expression,extension,family,finalize,force,format,forward,freeze,functions,generated,greatest,groups,handler,header,if,ilike,immutable,implicit,import,include,indent,index,indexes,inherit,inherits,inline,instead,isnull,json,json_array,json_arrayagg,json_exists,json_object,json_objectagg,json_query,json_scalar,json_serialize,json_table,json_value,keep,keys,label,leakproof,least,limit,listen,load,location,lock,locked,logged,mapping,materialized,merge_action,mode,move,nested,nfc,nfd,nfkc,nfkd,nothing,notify,notnull,nowait,off,offset,oids,omit,operator,owned,owner,parallel,parser,passing,password,plan,plans,policy,prepared,procedural,procedures,program,publication,quote,quotes,reassign,recheck,refresh,reindex,rename,replace,replica,reset,restrict,returning,routines,rule,scalar,schemas,sequences,server,setof,share,show,skip,snapshot,stable,standalone,statistics,stdin,stdout,storage,stored,strict,string,strip,subscription,support,sysid,tables,tablespace,target,temp,template,text,truncate,trusted,types,unconditional,unencrypted,unlisten,unlogged,until,vacuum,valid,validate,validator,variadic,verbose,version,views,volatile,whitespace,wrapper,xml,xmlattributes,xmlconcat,xmlelement,xmlexists,xmlforest,xmlnamespaces,xmlparse,xmlpi,xmlroot,xmlserialize,xmltable,yes";

    @Override
    public boolean isMatch(String sql) {
        return PREFIX.equalsIgnoreCase(sql.substring(0, Math.min(sql.length(), PREFIX.length())));
    }

    @Override
    public List<String> getColumns(String sql) {
        return List.of("string_agg");
    }

    @Override
    public List<List<String>> getRows(String sql) {
        return List.of(List.of(WORDS.split(",")));
    }
}
