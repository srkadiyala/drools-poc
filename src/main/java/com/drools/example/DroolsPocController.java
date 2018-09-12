package com.drools.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.drools.example.rules.ProductRules;

@RestController
public class DroolsPocController {
	
	@Autowired
	private DroolsPocService service;
	
	@RequestMapping(value="/discount", method=RequestMethod.POST)
	public ProductRules getDiscount(@RequestBody ProductRules productRules) {
		return service.getDiscountRules(productRules);
	}
}
