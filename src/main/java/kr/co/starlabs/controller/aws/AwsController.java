package kr.co.starlabs.controller.aws;

import java.util.ArrayList;
import java.util.Collection;
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

import com.amazonaws.auth.BasicAWSCredentials;

import kr.co.starlabs.service.aws.AwsService;

@Controller
public class AwsController {

	private static final Logger logger = LoggerFactory.getLogger(AwsController.class);

	@Autowired
	private AwsService awsService;

	@RequestMapping("/")
	public String main() {
		return "login";
	}

	// user 생성
	@RequestMapping("/createUser")
	public String createUser(Model model) {
		String username = "User_" + UUID.randomUUID().toString();
		Map<String, Object> resultMap = awsService.createUser(username);
		model.addAttribute("username", resultMap.get("username"));
		return "create";
	}

	@RequestMapping("/logout")
	public String logout(Model model) {

		awsService.logout();

		return "describe";
	}

	@RequestMapping("/listEc2")
	public String listEc2(Model model) {
		ArrayList<Object> resultList = awsService.listEC2();
		
		model.addAttribute("instances",resultList);
	
		return "list";
	}

	@RequestMapping("/descEc2")
	public String descEc2(Model model, @RequestParam("instance_id") String instance_id) {
		
		Map<String, Object> resultMap = awsService.descEC2(instance_id);

		model.addAttribute("instance_id", resultMap.get("instance_id"));
		model.addAttribute("ami", resultMap.get("ami"));
		model.addAttribute("type", resultMap.get("type"));
		model.addAttribute("state", resultMap.get("state"));
		model.addAttribute("monitoring_state", resultMap.get("monitoring_state"));
		model.addAttribute("launchTime", resultMap.get("launchTime"));

		return "describe";
	}

	@RequestMapping("/startEc2")
	public String startEc2(Model model) {

		String instance_id = "i-05030a3f53293474f";
		Map<String, Object> resultMap = awsService.startEC2(instance_id);

		model.addAttribute("instance_id", resultMap.get("instance_id"));
//		model.addAttribute("ami", resultMap.get("ami"));
//		model.addAttribute("type", resultMap.get("type"));
//		model.addAttribute("state", resultMap.get("state"));
//		model.addAttribute("monitoring_state", resultMap.get("monitoring_state"));
//		model.addAttribute("launchTime", resultMap.get("launchTime"));

		return "describe";
	}

	/**
	 * 
	 * @param model
	 * @param accessKeyId
	 * @param accessKeySecret
	 * @return
	 */
	@RequestMapping("/createEc2")
	public String createEc2(Model model) {
		// name은 태그에 사용할 이름

		String name = UUID.randomUUID().toString();
		String ami_id = "ami-0bea7fd38fabe821a";

		Map<String, Object> resultMap = awsService.createEC2(name, ami_id);
		model.addAttribute("reservation_id", resultMap.get("reservation_id"));
		model.addAttribute("ami_id", resultMap.get("ami_id"));

		return "describe";
	}

	@RequestMapping("/stopEc2")
	public String stopEc2(Model model) {

		String instance_id = "i-05030a3f53293474f";
		Map<String, Object> resultMap = awsService.stopEC2(instance_id);
		model.addAttribute("instance_id", resultMap.get("instance_id"));

		return "describe";
	}

}
