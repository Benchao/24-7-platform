package com.sindice.linker.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sindice.linker.cache.MemcachedClientWrapper;
import com.sindice.linker.sparql.SparqlResults;


@Controller
public class HomeController {

	private static final String prefix = "linker";
	private static final int DEFAULT_MDS_CACHEING_TIME_IN_SEC = 3600;
	private static final int DEFAULT_MDS_FUILURE_CACHEING_TIME_IN_SEC = 60*10;
	private Logger logger = LoggerFactory.getLogger(HomeController.class.getName());
	
	
    @RequestMapping(value="/")
    public String index(Model uiModel) {
    	uiModel.addAttribute("contextPrefix", "");
    	return "index";
    }

    /*
     * get the latest data from mds
     * cache the results  
     */
    @RequestMapping(value="/latest-jobs")
    @ResponseBody
    public ModelAndView latestJobs(Model uiModel,final HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) {
    	final String mdsUrl = (String) httpServletRequest.getSession().getServletContext().getAttribute(com.sindice.linker.servlet.AppServletConfigurationContextListener.MDS_URL);
    	final String mdsQueryLastJobs = (String) httpServletRequest.getSession().getServletContext().getAttribute(com.sindice.linker.servlet.AppServletConfigurationContextListener.MDS_QUERY_LAST_JOBS);
    	final String mdsQueryLastUserJobs = (String) httpServletRequest.getSession().getServletContext().getAttribute(com.sindice.linker.servlet.AppServletConfigurationContextListener.MDS_QUERY_LAST_USER_JOBS);
    	final String mdsQueryTotalGeneratedTriples = (String) httpServletRequest.getSession().getServletContext().getAttribute(com.sindice.linker.servlet.AppServletConfigurationContextListener.MDS_QUERY_TOTAL_GENERATED_TRIPLES);
    	
    	long mdsTimeout = 30;
    	
    	final String query;
        
    	if(httpServletRequest.getParameter("generatedTriples")!=null && httpServletRequest.getParameter("generatedTriples").equals("total")){
    		query = mdsQueryTotalGeneratedTriples;
    	}else{
        	if(httpServletRequest.getParameter("username")!=null){
        		query = mdsQueryLastUserJobs.replaceAll("##USERNAME##", httpServletRequest.getParameter("username").trim());
            }else{
            	query =  mdsQueryLastJobs;
            }
    	}
    	
    	
		Map<String, Object> res = new HashMap<String,Object>();
		res.put("status", "nok");
		res.put("error", "");
		
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<Object> task = new Callable<Object>() {
		   public Object call() {
		      return getLastJobs(mdsUrl,query,httpServletRequest);
		   }
		};
		
		Future<Object> future = executor.submit(task);
		try {
			res = (Map<String, Object>) future.get(mdsTimeout, TimeUnit.SECONDS); 
		} catch (TimeoutException e) {
		    // handle the timeout
			res.put("status", "nok");
			res.put("error", e.getMessage());
            logger.error("Not able to get posts",e);
		} catch (InterruptedException e) {
			res.put("status", "nok");
			res.put("error", e.getMessage());
            logger.error("Not able to get posts",e);
		} catch (ExecutionException e) {
			res.put("status", "nok");
			res.put("error", e.getMessage());
            logger.error("Not able to get posts",e);
		} finally {
		   future.cancel(true); // may or may not desire this
		}
		
		
    	uiModel.addAttribute("contextPrefix", "");
    	return JsonView.render(res, httpServletResponse);
    }

    private Map<String,Object> getLastJobs(String mdsUrl,String query, HttpServletRequest httpServletRequest){
    	MemcachedClientWrapper mc = (MemcachedClientWrapper) httpServletRequest.getSession().getServletContext().getAttribute(MemcachedClientWrapper.class.getName());
		HttpClient client = (HttpClient) httpServletRequest.getSession().getServletContext().getAttribute(HttpClient.class.getName());
		
    	
		
		String getUrl= mdsUrl;
		try {
			getUrl +="?query="+URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// should not happened 
			logger.error("Unsupported utf-8 encoding");
		}
		
    	Map<String,Object> res= new HashMap<String,Object>();
    	SparqlResults entries = new SparqlResults();
		
    	
		String key=null;
		if(mc!=null){
			key = mc.getCacheKey(prefix+"tweets" , httpServletRequest.getParameterMap());
		}
		
		if(mc!=null && mc.get(key)!=null){
			logger.info("Getting triples from cache for "+query);
			entries = (SparqlResults) mc.get(key);
			res.put("status", "ok");
			res.put("entries", entries);
		}else{
				logger.info("Will fetch triples from: "+mdsUrl +" and query:"+query);
				if(client!=null){
					GetMethod method = new GetMethod(getUrl);
					method.addRequestHeader(new Header("Accept","application/sparql-results+json"));
				
					try{
						int code = client.executeMethod(method);
						String json = method.getResponseBodyAsString();
						if(code==200){
							ObjectMapper mapper = new ObjectMapper();
						    entries = mapper.readValue(json,SparqlResults.class);
							
							res.put("status", "ok");
							res.put("entries", entries);
							// here store tweets in cache
							if(mc!=null){
								logger.info("Putting triples into cache for: "+DEFAULT_MDS_CACHEING_TIME_IN_SEC +" sec");
								mc.add(key, DEFAULT_MDS_CACHEING_TIME_IN_SEC, entries);
							}
						}else{
							res.put("status", "nok");
							logger.error("Error when getting latest jobs. Got response code"+code);
							logger.error("Body:\n"+json);
							// cache failure for some short time
							if(mc!=null){
								logger.info("Putting failure triples into cache for: "+DEFAULT_MDS_FUILURE_CACHEING_TIME_IN_SEC +" sec");
								mc.add(key, DEFAULT_MDS_FUILURE_CACHEING_TIME_IN_SEC, entries);
							}
						}
					}catch(Exception e){
						logger.error("Error when getting latest jobs",e);
						res.put("status", "nok");
						res.put("error", e.getMessage());
					}finally{
						if(method!=null){
							method.releaseConnection();
						}
					}
				}else{
					res.put("status", "nok");
					res.put("error", "no configured httpClient");
				}
		}
		return res;
    }
    
    @RequestMapping(value="/login/")
    public String login(Model uiModel) {
    	
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	if (authentication != null && authentication.isAuthenticated()) {
    		Collection<GrantedAuthority> authorities = authentication.getAuthorities();
    		for(GrantedAuthority a : authorities){
    			if(a.getAuthority().equals("ROLE_USER")){
    				return "redirect:/member/index";
    			}
    			if(a.getAuthority().equals("ROLE_ADMIN")){
    				return "redirect:/admin/users?page=1&size=10";
    			}
    		}
    	}
    	
    	uiModel.addAttribute("contextPrefix", "../");
    	return "login";
    }

    @RequestMapping(value="/linkset/**")
    public String linkset(Model uiModel,final HttpServletRequest httpServletRequest) {
    	
    	String restOfTheUrl = (String) httpServletRequest.getAttribute(
    	        HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    	
    	//take the second name
    	int index = restOfTheUrl.lastIndexOf("/");
    	String linksetName2 = restOfTheUrl.substring(index+1);
    	
    	String path = restOfTheUrl.substring(0,index);
    	int numberOfSlashes = StringUtils.countOccurrencesOf(restOfTheUrl,"/");
    	
    	String contextPrefix = "../";
    	for(int i=0;i<numberOfSlashes;i++){
    		contextPrefix+="../";
    	}
    	
    	// take the first name
    	index = path.lastIndexOf("/");
    	String linksetName1 = path.substring(index+1);
    	
    	
    	
    	uiModel.addAttribute("linkspecName1",linksetName1 );
    	uiModel.addAttribute("linkspecName2",linksetName2 );
    	uiModel.addAttribute("contextPrefix", contextPrefix);
    	uiModel.addAttribute("linksetUrl",path);
    	uiModel.addAttribute("fileServerUrl","http://demo.sindice.net/");
    	
    	return "linkset/index";
    }
    
    
}
