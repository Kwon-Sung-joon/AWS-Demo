package kr.co.starlabs.controller.aws;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import kr.co.starlabs.service.aws.AwsService;

@Controller
public class AwsController {

	private static final Logger logger = LoggerFactory.getLogger(AwsController.class);
	
	@Autowired
	private AwsService awsService;
	
/**
 * user Cognito
 * @param model
 * @return
 */

	
	@RequestMapping("/")
	public String main() {
		return "main";
	}
	//user 생성
	@RequestMapping("/createUser")
	public String createUser(Model model) {
		String username = UUID.randomUUID().toString();
		Map<String , Object> resultMap = awsService.createUser(username);
		model.addAttribute("username", resultMap.get("username"));
		
		
		return "main";
	}	
	@RequestMapping("/startEc2")
	public String startEc2(Model model) {

		String instance_id = "i-05030a3f53293474f";
		Map<String, Object> resultMap = awsService.startEC2(instance_id);
		
		return "main";
	}
	@RequestMapping("/stopEc2")
	public String stopEc2(Model model) {
		
		String instance_id = "i-05030a3f53293474f";
		Map<String, Object> resultMap = awsService.stopEC2(instance_id);
		
		return "main";
	}
	
	
	@RequestMapping("/terminteEc2")
	public String terminateEc2() {
		
		return "main";
	}
	

}
