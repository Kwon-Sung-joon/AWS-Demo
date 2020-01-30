package kr.co.starlabs.controller.aws;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kr.co.starlabs.service.aws.AwsService;

import java.util.ArrayList;

/**
 * 
 * @author sjkwon
 *
 */

@Controller
public class AwsController {

	private static final Logger logger = LoggerFactory.getLogger(AwsController.class);

	@Autowired
	private AwsService awsService;

	@RequestMapping("/")
	public String main() {
		return "login";
	}

	@RequestMapping("/loading")
	public String loading() throws Exception {

		return "loading";
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

		return "login";
	}

	@RequestMapping("/listEc2")
	public String listEc2(Model model) {

		ArrayList<Object> resultList = awsService.listEC2();

		model.addAttribute("instances", resultList);

		return "list";
	}

	@RequestMapping("/createEc2")
	public String createEc2(Model model) {

		String name = UUID.randomUUID().toString();

		Map<String, Object> resultMap = awsService.createEC2(name);
		model.addAttribute("instance_id", resultMap.get("instance_id"));

		return "main";
	}

	/**
	 * 
	 * @param model
	 * @param instance_id
	 * @return
	 */
	@RequestMapping("/startEc2")
	public String startEc2(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		Map<String, Object> resultMap = awsService.startEC2(instance_id);
		model.addAttribute("instance_id", resultMap.get("instance_id"));
		model.addAttribute("launchTime", "");

		return "main";
	}

	/**
	 * 
	 * @param model
	 * @param instance_id
	 * @return
	 */
	@RequestMapping("/stopEc2")
	public String stopEc2(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		Map<String, Object> resultMap = awsService.stopEC2(instance_id);
		model.addAttribute("instance_id", resultMap.get("instance_id"));

		return "main";
	}

	/**
	 * 
	 * @param model
	 * @param instance_id
	 * @return
	 */
	@RequestMapping("/terminateEc2")
	public String terminateEc2(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		Map<String, Object> resultMap = awsService.terminateEC2(instance_id);
		model.addAttribute("instance_id", resultMap.get("instance_id"));
		model.addAttribute("launchTime", "");
		return "main";
	}

	/**
	 * 
	 * @param model
	 * @param instance_id
	 * @return
	 */
	@RequestMapping("/descEc2")
	public String descEc2(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		Map<String, Object> resultMap = awsService.descEC2(instance_id);

		model.addAttribute("instance_id", resultMap.get("instance_id"));
		model.addAttribute("ami", resultMap.get("ami"));
		model.addAttribute("type", resultMap.get("type"));
		model.addAttribute("state", resultMap.get("state"));
		model.addAttribute("monitoring_state", resultMap.get("monitoring_state"));
		model.addAttribute("launchTime", resultMap.get("launchTime"));
		model.addAttribute("public_DNS", resultMap.get("public_DNS"));
		model.addAttribute("stateTransition", resultMap.get("stateTransition"));
//		model.addAttribute("stateTransitionMsg",resultMap.get("stateTransitionMsg"));
		return "main";
	}

	@RequestMapping("/logEc2")
	public String logEc2(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		ArrayList<Object> resultList = awsService.logEC2(instance_id);

		model.addAttribute("logs", resultList);
		model.addAttribute("instance_id", instance_id);
		/**
		 * log.html 생성, 로그 데이터 전달
		 */

		return "log";
	}

	@RequestMapping("/monitoringList")
	public String monitoringList(Model model, @RequestParam("instance_id") String instance_id) {
		logger.debug("instance_id [{}]", instance_id);
		ArrayList<Object> resultList = awsService.monitoringList(instance_id);
		model.addAttribute("metricNames", resultList);
		model.addAttribute("instance_id", instance_id);

		return "monitoringList";
	}

	@RequestMapping("/monitoringDesc")
	public String monitoringDesc(Model model, @RequestParam("instance_id") String instance_id,
			@RequestParam("metricName") String metricName) {
		logger.debug("monitoring params [{}]", instance_id, metricName);
		ArrayList<Object> resultList = awsService.monitoringDesc(instance_id, metricName);

		model.addAttribute("monitoring", resultList);
		model.addAttribute("metricName", metricName);
		model.addAttribute("instance_id", instance_id);

		return "monitoringDesc";
	}

}
