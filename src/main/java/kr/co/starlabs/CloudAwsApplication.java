package kr.co.starlabs;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CloudAwsApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(CloudAwsApplication.class);
	
	
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(CloudAwsApplication.class, args);

		if (logger.isDebugEnabled()){
			printBeanInformation(context);
		}
	}

	/**
	 * 스프링 빈 정보 출력: debug
	 * @param context
	 */
	private static void printBeanInformation(ApplicationContext context){
	    
		//스프링 빈 정보
		logger.debug("context bean information --------------------------------------------------------------");

		int i = 1;
		String[] beanNames = context.getBeanDefinitionNames();
		for (String name : beanNames) {			
			logger.debug("[{}] {}: {}", i++ ,name, context.getBean(name).getClass().getCanonicalName());
		}
	}
	
}
