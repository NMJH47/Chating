package com.example.webchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class WebchatApplication {

	public static void main(String[] args) throws UnknownHostException {
		ConfigurableApplicationContext application = SpringApplication.run(WebchatApplication.class, args);
		Environment env = application.getEnvironment();
		String host = InetAddress.getLocalHost().getHostAddress();
		String port = env.getProperty("server.port");
		System.out.println("[----------------------------------------------------------]");
		System.out.println("Group chat activated, click to enter:\t http://" + host + ":" + port);
		System.out.println("[----------------------------------------------------------");
		WebSocketServer.inst().run(53134);
	}

}
