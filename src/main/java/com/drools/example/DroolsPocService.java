package com.drools.example;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.drools.example.rules.ProductRules;

@Service
public class DroolsPocService implements ApplicationListener<EnvironmentChangeEvent> {
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private Environment environment;
	
	private String[] fileNames;
	private String ruleFileUrl;
	private KieContainer kieContainer;
	
	private Logger LOGGER  = LoggerFactory.getLogger(DroolsPocService.class);
	
	
	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		try {
			fileNames = environment.getProperty("rule.config.files").split(",");
			ruleFileUrl = environment.getProperty("rule.config.download.url");
			kieContainer = getKieContainer();
		} catch (Exception e) {
			LOGGER.error("Error creating a new session ", e);
		}
	}
	
	public ProductRules getDiscountRules(ProductRules productRules) {
		KieSession kieSession = kieContainer.newKieSession();
		kieSession.insert(productRules);
		kieSession.getAgenda().getAgendaGroup("PRODUCT").setFocus();
		kieSession.fireAllRules();
		kieSession.dispose();
		return productRules;
	}
	
	private KieContainer getKieContainer() throws Exception {
		
		KieServices kieServices = KieServices.Factory.get();
		KieFileSystem kfs = kieServices.newKieFileSystem();
		
		//load the rule flies
		for (String fileName : fileNames) {
			String downloadFileName = ruleFileUrl + fileName;
			ResponseEntity<org.springframework.core.io.Resource> resource = 
					restTemplate.getForEntity(downloadFileName, org.springframework.core.io.Resource.class);
			Resource r = ResourceFactory.newInputStreamResource(resource.getBody().getInputStream());
			kfs.write("src/main/resources/rules/"+fileName, r);
		}
		
		//build the content of the KieFileSystem by passing it to KieBuilder
		//it creates a KieModule
		KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
		kieBuilder.buildAll();
		
		//adds the KieModule resulting from the build to KieRepository
		KieRepository kieRepository = kieServices.getRepository();
		
		//create a new KieContainer with this KieModule using its ReleaseId
		KieContainer kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
		
		return kieContainer;
	}

}
