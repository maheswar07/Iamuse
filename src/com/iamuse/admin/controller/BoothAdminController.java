package com.iamuse.admin.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Blob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import com.iamuse.admin.VO.AdminPictureVO;
import com.iamuse.admin.VO.DeviceVO;
import com.iamuse.admin.VO.EventVO;
import com.iamuse.admin.VO.ImageEmailFormVO;
import com.iamuse.admin.VO.ImageFormVO;
import com.iamuse.admin.VO.OptionsReports;
import com.iamuse.admin.VO.PaginationVO;
import com.iamuse.admin.VO.SignInVO;
import com.iamuse.admin.VO.TransactionHistoryVO;
import com.iamuse.admin.VO.TransactionReceiptVO;
import com.iamuse.admin.entity.AdminBoothEventPicture;
import com.iamuse.admin.entity.Adminboothevent;
import com.iamuse.admin.entity.BoothAdminLogin;
import com.iamuse.admin.entity.DeviceRegistration;
import com.iamuse.admin.entity.Fovbyuser;
import com.iamuse.admin.entity.SubscriptionMaster;
import com.iamuse.admin.entity.TokenClsss;
import com.iamuse.admin.entity.UpdateSubscriptionReq;
import com.iamuse.admin.entity.UploadImage;
import com.iamuse.admin.service.BoothAdminService;
import com.iamuse.admin.service.IamuseDashboardService;
import com.iamuse.admin.service.SuperadminService;
import com.iamuse.admin.util.DateUtils;
import com.iamuse.admin.util.ImagePushNotificationTask;
import com.iamuse.admin.util.MailUtil;
import com.iamuse.admin.util.PushNotificationTask;
import com.iamuse.admin.util.PushNotificationTaskFOV;
import com.iamuse.admin.util.PushNotificationTaskImagesUpdate;
import com.iamuse.admin.util.ThreadPool;
import com.iamuse.admin.util.TweetUsingTwitter4jExample;
import com.iamuse.admin.util.ZohoContactsSynUtil;
import com.iamuse.admin.util.ZohoMarketingAddLeadToList;
import com.paypal.constants.ServerConstants;

import twitter4j.TwitterException;

@Controller
public class BoothAdminController {

	@Autowired
	BoothAdminService boothAdminService;
	@Autowired
	IamuseDashboardService iamuseDashboardService;
	@Autowired
	SuperadminService superadminService;
	@Autowired
	ThreadPool pool;
	@Autowired
	PushNotificationTask task;
	@Autowired
	PushNotificationTaskFOV fovTask;
	@Autowired
	PushNotificationTaskImagesUpdate taskImageUpdate;
	@Autowired
	ImagePushNotificationTask imagetask;
	@Autowired
	MessageSource messageSource;
	@Autowired
	MailUtil mailUtil;
	@Autowired
	private TweetUsingTwitter4jExample tweetUsingTwitter4jExample;
	/*
	 * @Autowired private ModelRepo repo;
	 */
	private static final Logger log = Logger.getLogger(BoothAdminController.class);

	List<DeviceRegistration> deviceRegistration;
	BoothAdminLogin boothAdminLogin;
	BoothAdminLogin boothAdminLogin1;
	BoothAdminLogin boothAdminLogin2;
	com.iamuse.admin.entity.AccessToken accessToken;
	String rootPaths = System.getProperty("catalina.home");

	@RequestMapping(value = "signUpPage")
	public String signUpPage(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		return "signUpPage";
	}

	@RequestMapping(value = "driveupload")
	public String driveupload(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		boothAdminLogin2 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
		boothAdminLogin2.setUserId(boothAdminLogin.getUserId());
		accessToken = boothAdminService.GetAccessToken(boothAdminLogin.getUserId());
		log.info("Access Token details="+accessToken);
		if (accessToken != null) {
			modelMap.addAttribute("accessToken", accessToken.getFlag());
		}
		modelMap.addAttribute("driveupload", boothAdminLogin2);
		return "driveupload";
	}

	@RequestMapping(value = "rgbSetup")
	public String rgbSetup(@ModelAttribute("ImageFormVO") ImageFormVO imageFormVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		List<ImageFormVO> imageFormVO1 = new ArrayList<>();
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
		deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
		List<ImageFormVO> imagesList = iamuseDashboardService.getImagesList("" + boothAdminLogin.getUserId());
		modelMap.addAttribute("imagesList", imagesList);
		if (boothAdminLogin1.getIsDefaultRgb() == ServerConstants.MAKE_TRUE
				&& boothAdminLogin1.getCurrentImageId() == 0) {
			imageFormVO.setHexValue(boothAdminLogin1.getHexValueDefault());
			imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueDefault());
			imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueDefault());
			String[] rgb = boothAdminLogin1.getRgbValueDefault().split(",");
			imageFormVO.setR(rgb[0]);
			imageFormVO.setG(rgb[1]);
			imageFormVO.setB(rgb[2]);
			imageFormVO1.add(imageFormVO);
			modelMap.addAttribute(ServerConstants.IMAGE_DETAILS, imageFormVO1);
			modelMap.addAttribute("id", "" + boothAdminLogin1.getCurrentImageId());
		} else if (boothAdminLogin1.getIsDefaultRgb() == ServerConstants.MAKE_FALSE
				&& boothAdminLogin1.getCurrentImageId() == 0) {
			imageFormVO.setHexValue(boothAdminLogin1.getHexValueManual());
			imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueManual());
			imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueManual());
			String[] rgb = boothAdminLogin1.getRgbValueManual().split(",");
			imageFormVO.setR(rgb[0]);
			imageFormVO.setG(rgb[1]);
			imageFormVO.setB(rgb[2]);
			imageFormVO1.add(imageFormVO);
			modelMap.addAttribute(ServerConstants.IMAGE_DETAILS, imageFormVO1);
			modelMap.addAttribute("id", "" + boothAdminLogin1.getCurrentImageId());
		} /*
			 * else{ imageFormVO.setHexValue(boothAdminLogin1.getHexValueManual());
			 * imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueManual());
			 * imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueManual()); String[]
			 * rgb=boothAdminLogin1.getRgbValueManual().split(",");
			 * imageFormVO.setR(rgb[0]); imageFormVO.setG(rgb[1]); imageFormVO.setB(rgb[2]);
			 * imageFormVO1.add(imageFormVO);
			 * modelMap.addAttribute(ServerConstants.IMAGE_DETAILS,imageFormVO1);
			 * modelMap.addAttribute("id",""+boothAdminLogin1.getCurrentImageId()); }
			 */
		modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
		modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
		return "rgbSetup";
	}

	@RequestMapping(value = "createBootAdmin")
	public String createBoothAdmin(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) throws ClientProtocolException, IOException {

				
	//	boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		 boothAdminLogin=null;

		if (boothAdminLogin == null) {
			SignInVO signInVOs = boothAdminService.createBoothAdmin(signInVO);
//			ZohoContactsSynUtil.zohoContactSync(signInVO.getEmailId());
//			ZohoMarketingAddLeadToList.addingLeadsToZohoMarketingHubSignUp(signInVO.getUsername(),
//					signInVO.getLastname(),
//					signInVO.getContactNumber(),
//					signInVO.getEmailId());
			if ((ServerConstants.SUCCESS).equals(signInVOs.getResult())) {
				boothAdminLogin1 = boothAdminService.getProfileDetails(signInVOs.getUserId());

				deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin1.getUserId());
				modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
				request.getSession().setAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin1);
				request.getSession().setAttribute("status", "0");
				request.getSession().setAttribute("oldListSize", 0);
				String emailbody = signInVO.getUsername();
				log.info("message sent:"+signInVO.getEmailId());
				String testText="<html><body>"+
						"<table id=\"m_-5368927744985068358backgroundTable\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
						"<tbody><tr><td><table id=\"m_-5368927744985068358innerTable\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
						"<tbody><tr><td class=\"m_-5368927744985068358payloadCell\" valign=\"top\"><table style=\"border:1px solid rgb(207,207,207);border-radius:8px;background:rgb(255,255,255)\" border=\"0\" cellspacing=\"0\" width=\"100%\">"+
						"<tbody><tr><td style=\"color:rgb(85,85,85);font-size:14px;font-family:'helvetica neue',arial,serif;padding:30px 10px\" align=\"center\">"+
		                "<h1 style=\"color:rgb(68,68,68);text-align:center;margin:0px;padding:0px\">Welcome to IAmuse</h1>"+
						"<p style=\"color:rgb(68,68,68);text-align:center;margin:0px;padding:0px\"><span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT100_com_zimbra_url\"><a href=\"http://www.iamuse.com\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=http://www.iamuse.com&amp;source=gmail&amp;ust=1493380684676000&amp;usg=AFQjCNExSsY9fpbaIXKUYmJaDURNeFlELw\"><img alt=\"iAmuse\" longdesc=\"https://ci3.googleusercontent.com/proxy/tSnkDkFiofgBYd5c5rsqAFQE_sTYbRIdlGOTJCekl9GkbR2Yz4vb0tMUMQ=s0-d-e1-ft#http://www.iamuse.com\" height=\"50\" width=\"150\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAN0AAABLCAYAAAD0+4JEAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNi1jMTQ1IDc5LjE2MzQ5OSwgMjAxOC8wOC8xMy0xNjo0MDoyMiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTkgKFdpbmRvd3MpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkJEMDM1Rjg1OENCRTExRTk4NkZGQTUyQTlGODkyRjQ2IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkJEMDM1Rjg2OENCRTExRTk4NkZGQTUyQTlGODkyRjQ2Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6QkQwMzVGODM4Q0JFMTFFOTg2RkZBNTJBOUY4OTJGNDYiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6QkQwMzVGODQ4Q0JFMTFFOTg2RkZBNTJBOUY4OTJGNDYiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4hzO4cAAAXdUlEQVR42uxd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\" class=\"CToWUd\"></a></span></p>"+
					    "<p style=\"font-weight:600;font-size:16px;text-align:left\">Hi "+emailbody+",</p>"+
						"<p style=\"font-weight:400;font-size:16px;text-align:left\">We hope you enjoy iAmuse PhotoBooth and check the images.</p>"+
					    "<p style=\"font-weight:600;font-size:16px;text-align:left\">Sincerely <br>Team iAmuse</p>"+
						"<table border=\"0\" cellpadding=\"5\" cellspacing=\"0\" width=\"100%\">"+
		                "<tbody><tr><td align=\"center\"> <p><span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT101_com_zimbra_url\"><a href=\"https://www.facebook.com/iamusebooth\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=https://www.facebook.com/iamusebooth&amp;source=gmail&amp;ust=1493380684677000&amp;usg=AFQjCNFLfToZNO2UisyTB9FWtiPfUFEhcA\"><img alt=\"\" height=\"77\" width=\"78\" src=\"data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAZABkAAD/2wBDAAMCAgICAgMCAgMEAgICBAQDAwMDBAUEBAQEBAUGBQYFBQYFBgYICAgICAYKCgsLCgoMDAwMDAwMDAwMDAwMDAz/2wBDAQMDAwUFBQoGBgoOCwkLDhAODg4OEBAMDAwMDBAQDAwMDAwMEAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAuAC4DAREAAhEBAxEB/8QAGAABAQEBAQAAAAAAAAAAAAAAAAgHBgn/xAArEAAABgEDAgUEAwAAAAAAAAAAAQIDBAUGBwgREiETFCIxdAk2OEGBs7T/xAAZAQEBAQEBAQAAAAAAAAAAAAAABQQGBwP/xAAsEQABAgQEBQIHAAAAAAAAAAAAAQIDBAURITFBcRITIjJRBsEzNIGhsdHw/9oADAMBAAIRAxEAPwDAx62cOAAAAAAAAAAABQuz/bZWa+ZDazsqkvw8Ow9MfzTUNRNyJkmUazbZSsyV0IJLSjWZFz7EXHPJRazU3SrERidTvsUKfJpGcquyQ1jdRontV0t02vYeIlDrNWq8q1yDXuXsuRP8N+Ywl1RxXZKyPlhS1d0e3qL9CdSZ6djRmq+6w1vdbJbJdbeTVPS0vDhrw92GpEQ6sigAAAAHrtt/wHRDBay1a0VnxbWLZuRXbY4duVqTbzaFk2SzJxzoMyUrt25/gea1GYmYqpz0VLZYWOtlYUJiLy13xuT/AL2NOdAGsezLPI1jEXrYt6rNcErpKpRKU/EjrI4PimfaNyfHT2L1C1Q5ma42Q1ReVjjbfXcwVKDB4XOv14a+2xBI7AggAAAAF8/TM+3M7+bVf0yBx3qjvZsvsXqN2u+hNm8X8lc4+XE/wRhdovybNl/Kk2ofHd/aGMioYwAAAACmtoW57Btv1TkkDLq62tXslkQpEZVOzGdShMZt1Kic8xJYMjM1lxxyINZpUSac1WKiWvnf9KUpCdZBRUci4+DJNetQKjVPVzIs/oWJMGoyN5h6OxPS2iShLUVlgycJpbqSPqbMy4UfYUqfLugQGw3Zp43Mk1FSJEV6ZKcANh8AAAAAAAAAAAAP/9k=\" class=\"CToWUd\"></a></span></p><p>Like us on Facebook</p></td>"+
		                "<td align=\"center\"><p><span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT102_com_zimbra_url\"><a href=\"http://instagram.com/iamusepics\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=http://instagram.com/iamusepics&amp;source=gmail&amp;ust=1493380684677000&amp;usg=AFQjCNF5OifpP40I6GUbWMZA7Wq8I0Y4mw\"><img alt=\"\" height=\"76\" width=\"77\" src=\"data:image/png;base64,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\" class=\"CToWUd\"></a></span></p><p>Follow us on Instagram</p></td>"+
		                "<td align=\"center\"><p><span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT103_com_zimbra_url\"><a href=\"http://instagram.com/iamusepics\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=http://instagram.com/iamusepics&amp;source=gmail&amp;ust=1493380684677000&amp;usg=AFQjCNF5OifpP40I6GUbWMZA7Wq8I0Y4mw\"><img alt=\"\" height=\"77\" width=\"78\" src=\"data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAZABkAAD/2wBDAAMCAgICAgMCAgMEAgICBAQDAwMDBAUEBAQEBAUGBQYFBQYFBgYICAgICAYKCgsLCgoMDAwMDAwMDAwMDAwMDAz/2wBDAQMDAwUFBQoGBgoOCwkLDhAODg4OEBAMDAwMDBAQDAwMDAwMEAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAuAC4DAREAAhEBAxEB/8QAGgABAAIDAQAAAAAAAAAAAAAAAAYHAwQIBf/EACkQAAEDAwMDBAIDAAAAAAAAAAECAwQABREGBxITIUEIFCJhMjdRcbX/xAAcAQEAAwADAQEAAAAAAAAAAAAABQYHAgMIBAH/xAAwEQACAQMCAgcHBQAAAAAAAAABAgADBBEFBiExBxJBUXGBoRMiMjVhgsGxsrPR4f/aAAwDAQACEQMRAD8Al1b/ADxjFIikRSIpEUiKRJdtNpS3633Cs+mLqpaLdclvqkdI8VlEeO7IKQfHLp4zUZrV69raPWT4hjHmQPzLBtXS6d/qdK2q56jE5xzwqlvXGJMvUTtjprby62xzSzbkODd2nC5GcdW8lDjRAyhThUrBB7gk1E7X1eteU2FYglTzxjn4cJZOkDbVrplambUFVcHIJJwR3E8fWU/VpmdxSIpEUiWR6dP3Lp7+5/8AnyagN0fLan2/uEufR988off/ABtOttX6w0Dpl+KzrKZEgvygpcUTG+oSlPZRT8FYFZhY2N3XBNBSQOeJ6A1bV9OtGUXjqpPw9YZ/BkQvW6uwjtomNyZltubC2XAqI1EKnHvifggdIfI+DkYPfNSlvo+rCopCsDnnnl6yvXm6NumiwZ6bDB90LxP0HDnOO7w9b5F2myLSyqHa35L7kNhZypqOpxRbQo+SlJANatQVxTUOcsAMnvPbPOt29NqztSHVQsSo7lzwHkJqV2T54pEmWz2oYelty7De7isMQY0hTL7quyW0SmlxytX0nqZNRWu2zV7KpTXiSOHkQfxLHtG/p2eq0a1Q4UNgnuDArny62Z0/vds7I3Wj26XaLg1b7jagtKPchSo7zLuD+TYUQRjt2Oc+Kznb+uCwZldSVbu5gjxm4bz2i2sLTak4V0zz+Eg+H+ynJ3pM1/FiPSY9wtk91hCnEx2nJCXHSkZ4I5shPI+MkD7q1096WjMAVYA9vDh6zOq/RXqSIWV6bEDkC2T9BlcZlMToUq2zZFunNmNNgOuRpDS/ybdaUULQfsEEVbqdRXUMpyCMjwMzatRelUam4wykgjuI4ETBXKdUUiKRLH0hv/uTo2A3a4Uxq6W2MODEe5tF8NpHYJSsKQsAY7DlgVAX22rK5cuylWPMqcf2PSXLSd+apY0xSRw6DkHHWx4HIbH0ziezcfVNufPhPw0Jt1uVKQpv3ESM6l5vkMcmy48sA/wSDXyUtn2KMG9447CRjz4CSVx0natUplB7NcjGVU5Hhlj+kqOTJkTJDsuU4qRKlLU8864SpbjiyVKUonuSSck1aUQKAqjAEz6pUZ2LscsTkk9pPMzHX7OEUiKRFIikRSIpE//Z\" class=\"CToWUd\"></a></span></p><p>Follow us on Twitter</p></td>"+
		                "</tr></tbody></table>"+
		                "<p>Visit our website <span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT104_com_zimbra_url\"><a href=\"http://www.iamuse.com\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=http://www.iamuse.com&amp;source=gmail&amp;ust=1493380684677000&amp;usg=AFQjCNE_v-p9Y1LQV-DpIv5GqwYEJDT-rQ\">www.iamuse.com</a></span></p>"+
		                "</td></tr></tbody></table></td></tr><tr>"+
		                "<td class=\"m_-5368927744985068358payloadCell\" style=\"height:40px;font-size:9px;font-family:'helvetica neue',arial,serif;color:rgb(136,136,136)\" align=\"right\" valign=\"top\"><span class=\"m_-5368927744985068358Object\" role=\"link\" id=\"m_-5368927744985068358OBJ_PREFIX_DWT105_com_zimbra_url\"><a style=\"color:rgb(136,136,136)\" href=\"http://iamuse.com\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?hl=en&amp;q=http://iamuse.com&amp;source=gmail&amp;ust=1493380684677000&amp;usg=AFQjCNHuUfOsnIEfdwOnnQQ9sl7Ljgn9ZA\">powered by iAmuse.com</a></span></td>"+
		                "</tr></tbody></table></td></tr></tbody></table></body></html>";
				mailUtil.sendEmailByInfo("IAMUSE <dev@iamuse.com>",signInVO.getEmailId(),testText,"Welcome to iAmuse");
				return ServerConstants.GET_SUBS;
			} else if (("exist").equals(signInVOs.getResult())) {
				if (signInVOs.getIsDeleted() == true) {
					
					boothAdminLogin1 = boothAdminService.getProfileDetails(signInVOs.getUserId());
					// boothAdminLogin1=boothAdminService.getProfileDetails(signInVOs.getUserId());
					deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin1.getUserId());
					modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
					request.getSession().setAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin1);
					request.getSession().setAttribute("status", "0");
					request.getSession().setAttribute("oldListSize", 0);
					return ServerConstants.GET_SUBS;
				} else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE, "Booth Admin already exists");
					return "redirect:signUpPage";
				}

			}
		}else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return "help";// ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "paymentMethodPage")
	public String paymentMethodPage(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			List<SubscriptionMaster> subscriptionMasters = boothAdminService.getSubscriptionList();
			modelMap.addAttribute("subscriptionMasters", subscriptionMasters);
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			return "paymentMethodPage";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "getDevices")
	public String getDevices(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) throws ParseException {
		return "redirect:getRegisteredDevice";
	}

	@RequestMapping(value = "getRegisteredDevice")
	public String getRegisteredDevice(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes)
			throws ParseException {
		String result1 = "registeredDevice";
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			if (("boothadmin").equalsIgnoreCase(boothAdminLogin.getUserRole())) {
				modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
				modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
				DeviceVO deviceVO = boothAdminService.grtDeviceTockenAndIP(boothAdminLogin.getUserId());
				modelMap.addAttribute("deviceVO", deviceVO);
				return "registeredDevice";
			} else if (("superadmin").equalsIgnoreCase(boothAdminLogin.getUserRole())) {
				modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
				modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
				result1 = "registeredDeviceSuperAdmin";
			}
			return result1;
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@ResponseBody
	@RequestMapping(value = "syncDevice")
	public String syncDevice(@RequestParam("deviceId") Integer deviceId, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes)
			throws ParseException {
		String result;
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			result = boothAdminService.syncDevice(boothAdminLogin1.getUserId(), deviceId);
			if ((ServerConstants.SUCCESS).equals(result)) {
				return "redirect:getRegisteredDeviceConfig";
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return result;
	}

	@RequestMapping(value = "setUpBackgroundImage")
	public String setUpBackgroundImage(@ModelAttribute("AdminPictureVO") AdminPictureVO adminPictureVO,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		String pageids = "";
		if (pageids != "") {
			pageids = request.getParameter("pictureId");
		} else {
			pageids = request.getParameter("picId");
		}
		String eventIds = request.getParameter("eId");
		String position = request.getParameter("position");

		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			PaginationVO paginationVO = boothAdminService.getFirstLast(Integer.parseInt(eventIds),
					adminPictureVO.getPicId());
			modelMap.addAttribute("first", paginationVO.getFirst());
			modelMap.addAttribute("current", adminPictureVO.getPicId());
			modelMap.addAttribute("last", paginationVO.getLast());
			modelMap.addAttribute("next", paginationVO.getNext());
			modelMap.addAttribute("previous", paginationVO.getPrevious());
			modelMap.addAttribute("position", position);
			AdminPictureVO adminPictureVO1 = boothAdminService.getImageConfigure(Integer.parseInt(pageids),
					boothAdminLogin1.getUserId(), boothAdminLogin1.getSubId());
			modelMap.addAttribute("adminPictureVO1", adminPictureVO1);
			modelMap.addAttribute("eid", Integer.parseInt(eventIds));
			List<AdminPictureVO> adminPictureVOs2 = boothAdminService.getPicList(Integer.parseInt(eventIds),
					boothAdminLogin.getUserId());
			modelMap.addAttribute("adminPictureVOs2", adminPictureVOs2);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			return "setUpBackground";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		
	}

	@RequestMapping(value = "saveCoordinatesOfImg")
	public String saveCoordinatesOfImg(
			@RequestParam(value = "files", required = ServerConstants.MAKE_FALSE) MultipartFile files,
			@ModelAttribute("AdminPictureVO") AdminPictureVO adminPictureVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) throws IOException {
		String result;
		String rootPath;
		String pid = "&position=";
		String eid = "&eId=";
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			PaginationVO paginationVO = boothAdminService.getFirstLast(adminPictureVO.getEId(),
					adminPictureVO.getPicId());
			adminPictureVO.setUpdatedBy(boothAdminLogin.getUserId());
			adminPictureVO.setSubId(boothAdminLogin1.getSubId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			rootPath = new java.io.File(request.getSession().getServletContext().getRealPath("") + "/.."
					+ "/iAmuse_images/Admin_Picture/Image_mask").getCanonicalPath();
			result = boothAdminService.saveCoordinatesOfImg(adminPictureVO, files, rootPath);
			if ((ServerConstants.SUCCESS).equals(result)) {
				Integer posupdate = adminPictureVO.getPosition();
				if (("Save & Exit").equalsIgnoreCase(adminPictureVO.getFinish())) {
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE,
							"Thank You !! \n Go to your device login and downloads the events");
					return "redirect:getUploadedImages?eventId=" + adminPictureVO.getEId();
				} else if (("Upload Now").equalsIgnoreCase(adminPictureVO.getFinish())) {
					return ServerConstants.SETUP_BACKGROUND_IMAGE + adminPictureVO.getPicId() + eid
							+ adminPictureVO.getEId() + pid + (posupdate - 1);
				} else if (("Previous").equalsIgnoreCase(adminPictureVO.getFinish())) {
					return ServerConstants.SETUP_BACKGROUND_IMAGE + paginationVO.getPrevious() + eid
							+ adminPictureVO.getEId() + pid + (adminPictureVO.getPosition() - 2);
				} else {
					return ServerConstants.SETUP_BACKGROUND_IMAGE + paginationVO.getNext() + eid
							+ adminPictureVO.getEId() + pid + adminPictureVO.getPosition();
				}
			}
			return ServerConstants.SETUP_BACKGROUND_IMAGE + paginationVO.getNext() + eid + adminPictureVO.getEId() + pid
					+ adminPictureVO.getPosition();
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "deleteEvent")
	public String deleteEvent(@RequestParam("eventId") Integer eid, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			String result = boothAdminService.deleteEvent(eid, boothAdminLogin.getUserId());
			if (result.equals(ServerConstants.SUCCESS)) {
				if (!deviceRegistration.isEmpty()) {
					ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
					taskImageUpdate.setDetails(deviceRegistration, messageSource, rootPaths);
					taskExecutor.execute(taskImageUpdate);
					return "redirect:getEventList";
				}
				return "redirect:getEventList";
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "eventReportDetails")
	public String eventReportDetails(@RequestParam("eventId") Integer eventId, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			/*
			 * if(boothAdminLogin1.getSubId()==1){ EventVO
			 * eventVO=boothAdminService.getEventDetails(boothAdminLogin.getUserId(),eventId
			 * ); modelMap.addAttribute("events",eventVO);
			 * modelMap.addAttribute("eventIdsss",eventId); OptionsReports
			 * optionsReports=boothAdminService.getEventReportDetailsDefault(boothAdminLogin
			 * .getUserId(),eventId,eventVO.getDefaultId());
			 * modelMap.addAttribute(ServerConstants.OPTIONS_REPORTS,optionsReports); }else{
			 */
			List<ImageEmailFormVO> emailImagesLists = iamuseDashboardService
					.getEventImagesSummaryLists("" + boothAdminLogin.getUserId(), eventId);
			modelMap.addAttribute("emailImagesLists", emailImagesLists);
      
			
			
			EventVO eventVO = boothAdminService.getEventDetails(boothAdminLogin.getUserId(), eventId);
			modelMap.addAttribute("events", eventVO);

			OptionsReports optionsReports = boothAdminService.getEventReportDetails(boothAdminLogin.getUserId(),
					eventId,boothAdminLogin.getEmailId());
			modelMap.addAttribute(ServerConstants.OPTIONS_REPORTS, optionsReports);

			List<ImageEmailFormVO> emailImagesList = iamuseDashboardService
					.getEmailImagesListBasedOnEventID(boothAdminLogin.getUserId(), eventId, eventVO.getEventName());
			modelMap.addAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);
			modelMap.addAttribute("eventIdsss", eventId);
			request.getSession().setAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);
			// }
			return "viewEventDatails";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "eventGallery")
	public String eventGallery(@RequestParam(value = "eventId", required = ServerConstants.MAKE_TRUE) Integer eventId,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		String pageids = request.getParameter(ServerConstants.PAGE_ID);
		String totals = request.getParameter(ServerConstants.TOTAL);
		int pageid = 0;
		int total = 0;
		if (pageids == null && totals == null) {
			pageid = 1;
			total = 51;
		}
		if (total == 0) {
			total = Integer.parseInt(totals);
			pageid = Integer.parseInt(pageids);
		}
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			List<ImageEmailFormVO> emailImagesList = iamuseDashboardService
					.getEmailImagesLists("" + boothAdminLogin.getUserId(), eventId, pageid, total);
			modelMap.addAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);

			OptionsReports optionsReports = boothAdminService.getEventReportDetails(boothAdminLogin.getUserId(),
					eventId);
			modelMap.addAttribute(ServerConstants.OPTIONS_REPORTS, optionsReports);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			int rCount = boothAdminService.getCountByEvent(boothAdminLogin.getUserId(), eventId);
			int pageCount;
			if (rCount % total == 0) {
				pageCount = rCount / total;
			} else {
				pageCount = rCount / total + 1;
			}
			if (emailImagesList.size() > 0) {
				modelMap.addAttribute("eventName", emailImagesList.get(0).getEventName());
			}
			modelMap.addAttribute(ServerConstants.PAGE_ID, pageid);
			modelMap.addAttribute("pageCount", pageCount);
			modelMap.addAttribute(ServerConstants.TOTAL, total);

			return "eventGallery";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "getProfileDetails")
	public String getProfileDetails(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);

			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
            
			List<SubscriptionMaster> subscriptionMaster = boothAdminService.getSubscriptionDetails();
			modelMap.addAttribute("subscriptionMaster", subscriptionMaster);
			
			return "myProfile";
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@ResponseBody
	@RequestMapping(value = "/imageDisplay", method = RequestMethod.GET)
	public void showImage(@RequestParam("id") Integer userId, HttpServletResponse response,
			HttpServletRequest request) {

		try {
			if (userId != 0) {
				boothAdminLogin1 = boothAdminService.getProfileDetails(userId);
				Blob imageBytes = boothAdminLogin1.getImage();
				int blobLength = (int) imageBytes.length();
				byte[] blobAsBytes = imageBytes.getBytes(1, blobLength);
				response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
				response.getOutputStream().write(blobAsBytes);
				response.getOutputStream().close();
			}
		} catch (Exception e) {
			log.info("BoothAdminController Method : imageDisplay");
			log.error("Error imageDisplay", e);
		}
	}

	@RequestMapping(value = "editProfileDetails")
	public String editProfileDetails(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			return "editMyProfile";
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "updateProfileDetails")
	public String updateProfileDetails(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes,
			@RequestParam("file") MultipartFile file) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		try {
			if (!file.isEmpty()) {
				if (file.getSize() > 0 && file.getSize() < 60000) {
					Blob blob = Hibernate.createBlob(file.getInputStream());
					signInVO.setImage(blob);
					signInVO.setImageFileName(file.getOriginalFilename());
				} else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE,
							"Upload Profile Images should be less than 60 KB size");
					return "redirect:editProfileDetails";
				}
			}
			String result = boothAdminService.updateProfileDetails(boothAdminLogin.getUserId(), signInVO);
			if (result.equals(ServerConstants.SUCCESS)) {
				redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE,
						"Your Profile Updated Successfully");
				return "redirect:getProfileDetails";
			}
		} catch (IOException e) {
			log.info("BoothAdminController Method : updateProfileDetails");
			log.error("Error updateProfileDetails", e);
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "getContactEmail")
	public String loginpage(@RequestParam("eventId") Integer eventId,
			@ModelAttribute("ImageEmailFormVO") ImageEmailFormVO imageEmailFormVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			// List<ImageEmailFormVO> emailImagesList =
			// iamuseDashboardService.getEmailImagesList(""+boothAdminLogin.getUserId(),eventId);
			List<ImageEmailFormVO> emailImagesList = iamuseDashboardService
					.getEmailImagesListCSV("" + boothAdminLogin.getUserId(), eventId);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			modelMap.addAttribute("eventId", eventId);
			modelMap.addAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);
			return "contactEmail";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "getSubscription")
	public String getSubscription(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) throws ParseException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);

		if (boothAdminLogin != null) {
			if (boothAdminLogin.getIsDeleted()) {
				return ServerConstants.REDIRECT_LOGIN_PAGE;
			}
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			if (("boothadmin").equalsIgnoreCase(boothAdminLogin.getUserRole())) {
				modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
				if (boothAdminLogin1.getSubUpdatedDate() != null && boothAdminLogin1.getSubId() == 1) {
					modelMap.addAttribute(ServerConstants.VALID_FROM, boothAdminLogin1.getSubUpdatedDate());
					modelMap.addAttribute("validTill", DateUtils.addDays(boothAdminLogin1.getSubUpdatedDate(), 0));
				}
				else if (boothAdminLogin1.getSubUpdatedDate() != null && boothAdminLogin1.getSubId() == 3) {
					modelMap.addAttribute(ServerConstants.VALID_FROM, boothAdminLogin1.getSubUpdatedDate());
					modelMap.addAttribute("validTill", DateUtils.addDays(boothAdminLogin1.getSubUpdatedDate(), 30));
				}
								
				modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
				modelMap.addAttribute(ServerConstants.VALID_FROM, boothAdminLogin1.getSubUpdatedDate());
				SubscriptionMaster subscriptionPlan = boothAdminService
						.getSubscriptionListById(boothAdminLogin1.getSubId());
				modelMap.addAttribute(ServerConstants.SUBSCRIPTION_PLAN, subscriptionPlan);
				modelMap.addAttribute("boothAdminLogin1", boothAdminLogin1);
				List<TransactionHistoryVO> transactionHistoryVOs = boothAdminService
						.getTransactionHistory(boothAdminLogin.getUserId());
				modelMap.addAttribute("transactionHistoryVOs", transactionHistoryVOs);
				List<SubscriptionMaster> subscriptionMaster = boothAdminService.getSubscriptionDetails();
				modelMap.addAttribute("subscriptionMaster", subscriptionMaster);
				return ServerConstants.SUBSCRIPTION;
		
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
			
		}
	return ServerConstants.SUBSCRIPTION;
	}

	@RequestMapping(value = "subscriptionPages")
	public String subscriptionPages(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		return "redirect:subscriptions";
	}

	@RequestMapping(value = "closeSubs")
	public String closeSubs(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		return ServerConstants.GET_SUBS;
	}

	@RequestMapping(value = "subscriptions")
	public String subscription(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin1);
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			if (boothAdminLogin.getSubUpdatedDate() != null) {
				modelMap.addAttribute(ServerConstants.VALID_FROM, boothAdminLogin1.getSubUpdatedDate());
				modelMap.addAttribute("validTill", DateUtils.addDays(boothAdminLogin.getSubUpdatedDate(), 30));
				SubscriptionMaster subscriptionPlan = boothAdminService
						.getSubscriptionListById(boothAdminLogin1.getSubId());
				modelMap.addAttribute(ServerConstants.SUBSCRIPTION_PLAN, subscriptionPlan);
				return ServerConstants.SUBSCRIPTION;
			}
			SubscriptionMaster subscriptionPlan = boothAdminService
					.getSubscriptionListById(boothAdminLogin1.getSubId());
			modelMap.addAttribute(ServerConstants.SUBSCRIPTION_PLAN, subscriptionPlan);
			return ServerConstants.SUBSCRIPTION;
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "upgradePlan")
	public String upgradePlan(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		} else {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			List<SubscriptionMaster> subscriptionMaster = boothAdminService.getSubscriptionDetails();
			modelMap.addAttribute("subscriptionMaster", subscriptionMaster);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			return "upgradePlan";
		}
	}

	@RequestMapping(value = "boothSetUp")
	public String boothSetUp(@ModelAttribute("ImageFormVO") ImageFormVO imageFormVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
		modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
		boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
		List<ImageFormVO> imageFormVO1 = new ArrayList<>();
		List<ImageFormVO> imagesList = iamuseDashboardService.getImagesList("" + boothAdminLogin.getUserId());
		modelMap.addAttribute("imagesList", imagesList);
		if (boothAdminLogin1.getIsDefaultRgb() == ServerConstants.MAKE_TRUE
				&& boothAdminLogin1.getCurrentImageId() == 0) {
			imageFormVO.setHexValue(boothAdminLogin1.getHexValueDefault());
			imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueDefault());
			imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueDefault());

			String[] rgb = boothAdminLogin1.getRgbValueDefault().split(",");
			imageFormVO.setR(rgb[0]);
			imageFormVO.setG(rgb[1]);
			imageFormVO.setB(rgb[2]);
			imageFormVO1.add(imageFormVO);
			modelMap.addAttribute(ServerConstants.IMAGE_DETAILS, imageFormVO1);
			modelMap.addAttribute("id", "" + boothAdminLogin1.getCurrentImageId());
		} else if (boothAdminLogin1.getIsDefaultRgb() == ServerConstants.MAKE_FALSE
				&& boothAdminLogin1.getCurrentImageId() == 0) {
			imageFormVO.setHexValue(boothAdminLogin1.getHexValueManual());
			imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueManual());
			imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueManual());

			String[] rgb = boothAdminLogin1.getRgbValueManual().split(",");
			imageFormVO.setR(rgb[0]);
			imageFormVO.setG(rgb[1]);
			imageFormVO.setB(rgb[2]);
			imageFormVO1.add(imageFormVO);
			modelMap.addAttribute(ServerConstants.IMAGE_DETAILS, imageFormVO1);
			modelMap.addAttribute("id", "" + boothAdminLogin1.getCurrentImageId());
		}
		/*
		 * else{ imageFormVO.setHexValue(boothAdminLogin1.getHexValueManual());
		 * imageFormVO.setRgbValue(boothAdminLogin1.getRgbValueManual());
		 * imageFormVO.setRgbaValue(boothAdminLogin1.getRgbaValueManual());
		 * 
		 * String rgb[]=boothAdminLogin1.getRgbValueManual().split(",");
		 * imageFormVO.setR(rgb[0]); imageFormVO.setG(rgb[1]); imageFormVO.setB(rgb[2]);
		 * imageFormVO1.add(imageFormVO);
		 * modelMap.addAttribute(ServerConstants.IMAGE_DETAILS,imageFormVO1);
		 * modelMap.addAttribute("id",""+boothAdminLogin1.getCurrentImageId()); }
		 */
		modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
		UploadImage uploadImage = boothAdminService.getCurrentImages(boothAdminLogin.getUserId());
		if (uploadImage.getImageName() != null) {
			modelMap.addAttribute(ServerConstants.UPLOAD_IMAGE, uploadImage);
		} else {
			modelMap.addAttribute("hide", "hide");
		}
		return "boothSetUp";
	}

	@RequestMapping(value = "advanceBoothSetUp")
	public String advanceBoothSetUp(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			signInVO.setUserId(boothAdminLogin.getUserId());
			String result = boothAdminService.advanceBoothSetUp(signInVO);
			if (result.equals(ServerConstants.SUCCESS)) {
				redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE,
						"Your event fov booth setup successfully");
				return ServerConstants.BOOTH_SETUP;
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return ServerConstants.BOOTH_SETUP;
	}

	@RequestMapping(value = "detailSubscriptionPlan")
	public String getUpgradeSubscriptionPlan(
			@RequestParam(value = "id", required = ServerConstants.MAKE_FALSE) Integer id, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			SubscriptionMaster subscriptionPlan = boothAdminService.getSubscriptionListById(id);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			modelMap.addAttribute(ServerConstants.SUBSCRIPTION_PLAN, subscriptionPlan);
			if (id == 2 || id == 3) {
				modelMap.addAttribute(ServerConstants.SUBSCRIPTION_PLAN, subscriptionPlan);
				return "upgradeSubscriptionPlan";
			}
			return "upgradeSubscriptionPlan";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "/saveSubscriptionDetailsPP")
	public String saveEventTicketDetailsPP(
			@ModelAttribute("TransactionReceiptVO") TransactionReceiptVO transactionReceiptVOs,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			try {

				MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
				map.add("cmd", transactionReceiptVOs.getCmd());
				map.add("business", transactionReceiptVOs.getBusiness());
				map.add("item_name", transactionReceiptVOs.getItem_name());
				map.add("item_number", transactionReceiptVOs.getItem_number());
				map.add("amount", transactionReceiptVOs.getAmount());
				map.add("return", transactionReceiptVOs.getNotify_url());
				map.add("rm", "2");
				map.add("cancel_return", transactionReceiptVOs.getCancel_return());
				map.add("currency_code", "USD");
				map.add("quantity", "1");
				map.add("payment_date", new Date().toString());
				map.add("night_phone_a", transactionReceiptVOs.getNight_phone_a());
				map.add("email", transactionReceiptVOs.getEmail());
				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.add("Content-Type", "application/x-www-form-urlencoded");
				HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map,
						headers);

				ResponseEntity<String> result = restTemplate.exchange("https://www.sandbox.paypal.com/in/cgi-bin/webscr",
//				ResponseEntity<String> result = restTemplate.exchange("https://www.paypal.com/cgi-bin/webscr",
						HttpMethod.POST, entity, String.class);
				log.info("Resutl-paypal"+result);
				log.info("result body"+result.getBody());

				String[] loc = result.getBody().split("Found. Redirecting to ");
				String location = "";
				for (String loca : loc) {
					location = loca;
				}

				return "redirect:" + location;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				log.info("BoothAdminController Method : saveSubscriptionDetails");
				log.error("Error saveSubscriptionDetails", e);
			}
		}
		return "redirect:";
	}

	@RequestMapping(value = "/saveSubscriptionDetails")
	public String saveEventTicketDetails(
			@ModelAttribute("TransactionReceiptVO") TransactionReceiptVO transactionReceiptVOs,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			try {
				String cmd = "cmd=_notify-validate";
				String paramName;
				String paramValue;
				Enumeration<?> en = request.getParameterNames();
				while (en.hasMoreElements()) {
					paramName = (String) en.nextElement();
					paramValue = request.getParameter(paramName);
					cmd = cmd + "&" + paramName + "=" + URLDecoder.decode(paramValue, "UTF-8");
				}
				URL u = new URL("https://ipnpb.sandbox.paypal.com/cgi-bin/webscr");
//				URL u = new URL("https://ipnpb.paypal.com/cgi-bin/webscr");
				HttpsURLConnection uc = (HttpsURLConnection) u.openConnection();
				uc.setRequestMethod("POST");
				uc.setDoInput(ServerConstants.MAKE_TRUE);
				uc.setDoOutput(ServerConstants.MAKE_TRUE);
				uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				// uc.setRequestProperty("Host", "www.paypal.com");

				PrintWriter pw = new PrintWriter(uc.getOutputStream());
				pw.println(cmd);
				pw.close();

				// 4. Read response from Paypal
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String res = in.readLine();
				in.close();

				TransactionReceiptVO transactionReceiptVO = boothAdminService
						.setTransactionHistoryOfSubscription(boothAdminLogin.getUserId(), request, res, cmd);
				if (transactionReceiptVO.getResult().equals(ServerConstants.SUCCESS)
						&& boothAdminLogin.getSubId() == Integer.parseInt(transactionReceiptVO.getSubId())) {
					  if(transactionReceiptVO.getAmount().equals("1200.00"))
					  {
						  return "redirect:paymentReciepts?trsId=" + transactionReceiptVO.getTrsId() + "&subId="
									+ transactionReceiptVO.getSubId();
					  }
					  else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE, "You have Already Subscribed");
					return ServerConstants.GET_SUBS;
					  }
				} else if (("INVALID").equals(transactionReceiptVO.getResult())) {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE,
							"Invalid Transaction Please Contact To Merchant" + transactionReceiptVO.getTrsId());
					return ServerConstants.GET_SUBS;
				} else {
					return "redirect:paymentReciepts?trsId=" + transactionReceiptVO.getTrsId() + "&subId="
							+ transactionReceiptVO.getSubId();
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
				log.info("BoothAdminController Method : saveSubscriptionDetails");
				log.error("Error saveSubscriptionDetails", e);
			}
			return ServerConstants.GET_SUBS;
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "/paymentReciepts")
	public String getTransactionDetails(@RequestParam("trsId") String trsId, @RequestParam("subId") String subId,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			try {
			TransactionReceiptVO transactionReceiptVO = boothAdminService.getTransactionDetails(trsId, subId);
			if (transactionReceiptVO.getResult().equals(ServerConstants.SUCCESS)) {
				modelMap.addAttribute("transactionReceiptVO", transactionReceiptVO);
				return "paymentReciept";
			}
			}
			catch(HibernateException he) {
				he.printStackTrace();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return "paymentReciept";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "dbToCsv")
	public String dbToCsv(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) throws IOException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		int pageid = Integer.parseInt(request.getParameter(ServerConstants.PAGE_ID));
		int total = Integer.parseInt(request.getParameter(ServerConstants.TOTAL));
		int p;
		if (pageid == 1)
			p = 0;
		else
			p = ((pageid - 1) * total);

		if (boothAdminLogin != null) {

			try {
				String csvFileName = "EmailRecord.csv";
				response.setContentType("text/csv");
				// creates mock data
				String headerKey = ServerConstants.CONTENT_DISPOSITION;
				String headerValue = String.format("attachment; filename=\"%s\"", csvFileName);
				response.setHeader(headerKey, headerValue);
				// uses the Super CSV API to generate CSV data from the model data
				ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
				String[] header = { "userName", ServerConstants.EVENT_NAME, "contactNo", "emailId" };
				csvWriter.writeHeader(header);
				csvWriter.close();

			} catch (Exception e) {
				log.info("BoothAdminController Method : dbToCsv");
				log.error("Error dbToCsv", e);
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return null;
	}

	@RequestMapping(value = "dbToImagesZip")
	public String dbToImagesZip(@ModelAttribute("ImageEmailFormVO") ImageEmailFormVO imageEmailFormVO,
			@RequestParam(value = "eventId", required = false) Integer eventId, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) throws IOException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		String name2;
		FileInputStream fin;
		if (boothAdminLogin != null) {
			String[] total = imageEmailFormVO.getImageIds();
			if (("export").equals(imageEmailFormVO.getEventAction())) {
				List<ImageEmailFormVO> emailImagesList = iamuseDashboardService
						.getEmailImagesZipList("" + boothAdminLogin.getUserId(), total);
				name2 = new java.io.File(request.getSession().getServletContext().getRealPath("") + "/..")
						.getCanonicalPath();
				try {
					String date = FastDateFormat.getInstance("MM-dd-yyyy").format(System.currentTimeMillis());
					String zipFile = "iAmuse-" + date + "-" + imageEmailFormVO.getEventName() + ".zip";
					response.setHeader(ServerConstants.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile + "\"");
					response.setContentType("application/zip");
					ServletOutputStream outputStream = response.getOutputStream();
					byte[] buffer = new byte[1024];
					ZipOutputStream zout = new ZipOutputStream(outputStream);
					for (int i = 0; i < total.length; i++) {
						fin = new FileInputStream(name2 + emailImagesList.get(i).getMailImageUrl() + "/"
								+ emailImagesList.get(i).getEventId() + "/"
								+ emailImagesList.get(i).getMailImageName());
						zout.putNextEntry(new ZipEntry(emailImagesList.get(i).getMailImageName()));
						int length;
						while ((length = fin.read(buffer)) > 0) {
							zout.write(buffer, 0, length);
						}
						zout.closeEntry();
						fin.close();
					}
					zout.close();
					modelMap.addAttribute(ServerConstants.SUCCESS_MESSAGE, "Export Images Successfully");
				} catch (IOException ioe) {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE,
							"fail for exporting the selected images succesfully ");
					log.info("BoothAdminController Method : dbToImagesZip");
					log.error("Error dbToImagesZip", ioe);
				}
			} else if (("delete").equals(imageEmailFormVO.getEventAction())) {
				String result = iamuseDashboardService.deleteMailedImage("" + boothAdminLogin.getUserId(), total);
				if (result.equals(ServerConstants.SUCCESS)) {
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Selected Images Deleted");
					return ServerConstants.EVENT_GALLERY + imageEmailFormVO.getEventId();
				} else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE, "Images Not Deleted");
					return ServerConstants.EVENT_GALLERY + imageEmailFormVO.getEventId();
				}
			} else if (("resend").equals(imageEmailFormVO.getEventAction())) {

				String result = boothAdminService.resendEmailImages("" + boothAdminLogin.getUserId(), total, request);
				if (result.equals(ServerConstants.SUCCESS)) {
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Email Sent Successfully!");
					return ServerConstants.EVENT_GALLERY + imageEmailFormVO.getEventId();
				} else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE, result);
					return ServerConstants.EVENT_GALLERY + imageEmailFormVO.getEventId();
				}
			}
			return ServerConstants.EVENT_GALLERY + imageEmailFormVO.getEventId();
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "socialShare")
	public String shareFbOrTwiter(@RequestParam("userId") String u, @RequestParam("imageIds") String[] imageIds,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		if (!("null").equals(u)) {
			String[] total = imageIds;
			List<ImageEmailFormVO> emailImagesList = iamuseDashboardService.getEmailImagesShareList("" + u, total);
			if (!emailImagesList.isEmpty()) {
				modelMap.addAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);
				return "socialShare";
			}
			String referer = request.getHeader("Referer");
			return "redirect:" + referer;
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "/NotifyPage")
	public String NotifyPage(@ModelAttribute("EventVO") EventVO eventVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			try {

				redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE,
						"Your payment was unsuccessful.Please contact your merchant.");
				return "redirect:getSubscription";
			} catch (Exception e) {
				log.info("BoothAdminController Method : NotifyPage");
				log.error("Error NotifyPage", e);
			}
			return ServerConstants.GET_SUBS;
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "saveAdminDetails")
	public String saveAdminDetails(@ModelAttribute("EventVO") EventVO eventVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		eventVO.setCreatedBy(boothAdminLogin.getUserId());
		if (boothAdminService.setAdminDetails(eventVO)) {
			redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Update Event Summary Successfully");
			return "redirect:eventReportDetails?eventId=" + eventVO.getEId();
		}
		return "redirect:eventReportDetails?eventId=" + eventVO.getEId();
	}

	@ResponseBody
	@RequestMapping(value = "sendTestMail", method = RequestMethod.GET)
	public boolean sendTestMail(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "email", required = ServerConstants.MAKE_FALSE) String email,
			RedirectAttributes redirectAttributes) {
		boolean results = ServerConstants.MAKE_FALSE;
		try {
			if (mailUtil.sendTestEmail("iAmuse <dev@iamuse.com>", email, "Test Mail")) {
				results = ServerConstants.MAKE_TRUE;
			} else {
				results = ServerConstants.MAKE_FALSE;
			}
		} catch (Exception e) {
			log.info("BoothAdminController Method : sendTestMail");
			log.error("Error sendTestMail", e);
			e.getMessage();
		}
		return results;
	}

	@ResponseBody
	@RequestMapping(value = "fetchContactList", method = RequestMethod.GET)
	public List<ImageEmailFormVO> emailImagesList(HttpServletRequest request, HttpServletResponse response,
			ModelMap modelMap,
			@RequestParam(value = "selectedEventId", required = ServerConstants.MAKE_FALSE) String selectedEventId,
			@RequestParam(value = "selectedEventName", required = ServerConstants.MAKE_FALSE) String selectedEventName) {
		return iamuseDashboardService.getEmailImagesListBasedOnEventID(boothAdminLogin.getUserId(),
				Integer.parseInt(selectedEventId), selectedEventName);
	}

	@RequestMapping(value = "boothSetUpByEvent", method = RequestMethod.GET)
	public String boothSetUpByEvent(
			@RequestParam(value = "eventId", required = ServerConstants.MAKE_FALSE) Integer eventId,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
		modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
		deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
		modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
		Adminboothevent adminboothevent = boothAdminService.getEventDetails(eventId);
		modelMap.addAttribute("adminboothevent", adminboothevent);
		modelMap.addAttribute(ServerConstants.EVENT_NAME, adminboothevent.getEventName());
		modelMap.addAttribute("eventId", eventId);
		UploadImage uploadImage = boothAdminService.getCurrentImages(boothAdminLogin.getUserId());
		if (uploadImage.getImageName() != null) {
			modelMap.addAttribute(ServerConstants.UPLOAD_IMAGE, uploadImage);
		} else {
			modelMap.addAttribute("hide", "hide");
		}
		return "boothSetUpByEvent";
	}

	@RequestMapping(value = "advanceBoothSetUpByEvent")
	public String advanceBoothSetUpByEvent(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin1 != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			Adminboothevent adminboothevent = boothAdminService.getEventDetails(signInVO.getEId());
			modelMap.addAttribute("adminboothevent", adminboothevent);
			signInVO.setUserId(boothAdminLogin.getUserId());
			String result = boothAdminService.advanceBoothSetUp(signInVO);
			if (result.equals(ServerConstants.SUCCESS)) {
				return "redirect:rgbSetup";
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return ServerConstants.BOOTH_SETUP;
	}

	@RequestMapping(value = "finishConfiguration")
	public String finishConfiguration(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Thank You");
		return "redirect:getSubscribedEventList";
	}

	@ResponseBody
	@RequestMapping(value = "updateMaskingImageStatus", method = RequestMethod.GET)
	public String updateMaskingImageStatus(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "pictureId", required = ServerConstants.MAKE_FALSE) Integer pictureId) {
		String result = null;
		if (boothAdminService.updateMaskingImageStatus(pictureId)) {
			result = "Update Image Mask";
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "updateWaterMarkStatus", method = RequestMethod.GET)
	public String updateWaterMarkStatus(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "pictureId", required = ServerConstants.MAKE_FALSE) Integer pictureId) {
		String result = null;
		if (boothAdminService.updateWaterMarkStatus(pictureId)) {
			result = "Update Image Mask";
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "setFbShareValue", method = RequestMethod.GET)
	public String setShareValue(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "userId", required = ServerConstants.MAKE_FALSE) int userId,
			@RequestParam(value = "imagesId", required = ServerConstants.MAKE_FALSE) String imagesId) {
		String[] imagesIdList = imagesId.split(",");
		return boothAdminService.setShareValue(userId, imagesIdList);
	}

	@ResponseBody
	@RequestMapping(value = "setTwitterShareValue", method = RequestMethod.GET)
	public String setTwitterShareValue(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "userId", required = ServerConstants.MAKE_FALSE) int userId,
			@RequestParam(value = "imagesId", required = ServerConstants.MAKE_FALSE) String imagesId)
			throws MalformedURLException, IOException, TwitterException {
		String[] imagesIdList = imagesId.split(",");
		return boothAdminService.setTwitterShareValue(userId, imagesIdList);
	}

	@RequestMapping(value = "exportsContact")
	public String exportsContact(@RequestParam("eventId") Integer eventId, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) throws IOException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			// List<ImageEmailFormVO>
			// emailImagesList=(List<ImageEmailFormVO>)request.getSession().getAttribute(ServerConstants.EMAIL_IMAGE_LIST);
			List<ImageEmailFormVO> emailImagesList = iamuseDashboardService
					.getEmailImagesListCSV("" + boothAdminLogin.getUserId(), eventId);
			OptionsReports optionsReports = boothAdminService.getEventReportDetails(boothAdminLogin.getUserId(),
					eventId);
			try {
				String csvFileName = "ExportsContact.csv";
				response.setContentType("text/csv");
				// creates mock data
				String headerKey = ServerConstants.CONTENT_DISPOSITION;
				String headerValue = String.format("attachment; filename=\"%s\"", csvFileName);
				response.setHeader(headerKey, headerValue);
				// uses the Super CSV API to generate CSV data from the model data
				ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
				String[] header = { "userName", ServerConstants.EVENT_NAME, "contactNo", "eventDate", "emailId",
						"subscribed" };
				csvWriter.writeHeader(header);
				for (ImageEmailFormVO eventL : emailImagesList) {
					csvWriter.write(eventL, header);
				}
				String[] header2 = { "totalGuestSessions", "totalGuests", "repeatGuests", "photosSent", "emailsSent",
						"avgVisitorSession", "SignUps", "emailBounces", "facebook", "twitter" };
				csvWriter.writeHeader(header2);
				csvWriter.write(optionsReports, header2);
				csvWriter.close();

			} catch (Exception e) {
				log.info("BoothAdminController Method : exportsContact");
				log.error("Error exportsContact", e);
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return null;
	}

	@RequestMapping(value = "saveRGBValueBoothSetup", params = { "id" })
	public String saveRGBValueBoothSetup(@ModelAttribute("ImageFormVO") ImageFormVO imageFormVO,
			@RequestParam(value = "id", required = false) String imageId, HttpServletRequest request,
			HttpServletResponse response, ModelMap model, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		boolean result;
		String checkMinOrMax;
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		try {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			String rgbval = request.getParameter("rgbValue");
			String hexval = request.getParameter("hexValue");
			String rgbaval = request.getParameter("rgbaValue");
			checkMinOrMax = request.getParameter("rgb");
			if (checkMinOrMax == null) {
				checkMinOrMax = "default";
			}
			if (("min").equals(checkMinOrMax) && ("default").equals(checkMinOrMax)) {
				imageFormVO.setImageId(Integer.parseInt(imageId));
				imageFormVO.setRgbValue(rgbval);
				imageFormVO.setHexValue(hexval);
				imageFormVO.setRgbaValue(rgbaval);
				if (imageFormVO.getRgbValue() == null || ("").equals(imageFormVO.getRgbValue())) {
					imageFormVO.setRgbValue(0 + "," + 0 + "," + 0);
				}
				result = iamuseDashboardService.updateRGBValue(imageFormVO, "" + boothAdminLogin1.getUserId());
				if (result) {
					boothAdminLogin1.setHexValueManual(hexval);
					if (rgbval == null || ("").equals(rgbval)) {
						boothAdminLogin1.setRgbValueManual(0 + "," + 0 + "," + 0);
					} else {
						boothAdminLogin1.setRgbValueManual(rgbval);
					}
					boothAdminLogin1.setRgbaValueManual(rgbaval);
					boothAdminLogin1.setIsDefaultRgb(ServerConstants.MAKE_FALSE);
					request.getSession().setAttribute(ServerConstants.USER_MASTER, boothAdminLogin1);
				}
			} else if (("max").equals(checkMinOrMax)) {
				imageFormVO.setImageId(Integer.parseInt(imageId));
				imageFormVO.setRgbValue(rgbval);
				imageFormVO.setHexValue(hexval);
				imageFormVO.setRgbaValue(rgbaval);
				if (imageFormVO.getRgbValue() == null || ("").equals(imageFormVO.getRgbValue())) {
					imageFormVO.setRgbValue(0 + "," + 0 + "," + 0);
				}
				result = iamuseDashboardService.updateRGBValueMax(imageFormVO, "" + boothAdminLogin1.getUserId());
				if (result) {
					boothAdminLogin1.setIsDefaultRgb(ServerConstants.MAKE_FALSE);
					boothAdminLogin1.setHexValueManual(hexval);
					boothAdminLogin1.setRgbaValueManual(rgbaval);
					if (rgbval == null || ("").equals(rgbval)) {
						boothAdminLogin1.setRgbValueManual(0 + "," + 0 + "," + 0);
					} else {
						boothAdminLogin1.setRgbValueManual(rgbval);
					}
					request.getSession().setAttribute(ServerConstants.USER_MASTER, boothAdminLogin1);
				}
			} else {
				imageFormVO.setImageId(Integer.parseInt(imageId));
				imageFormVO.setRgbValue(rgbval);
				imageFormVO.setHexValue(hexval);
				imageFormVO.setRgbaValue(rgbaval);
				if (imageFormVO.getRgbValue() == null || ("").equals(imageFormVO.getRgbValue())) {
					imageFormVO.setRgbValue(0 + "," + 0 + "," + 0);
				}
				result = iamuseDashboardService.updateRGBValue(imageFormVO, "" + boothAdminLogin1.getUserId());
				if (result) {
					boothAdminLogin1.setHexValueManual(hexval);
					if (rgbval == null || ("").equals(rgbval)) {
						boothAdminLogin1.setRgbValueManual(0 + "," + 0 + "," + 0);
					} else {
						boothAdminLogin1.setRgbValueManual(rgbval);
					}
					boothAdminLogin1.setRgbaValueManual(rgbaval);
					boothAdminLogin1.setIsDefaultRgb(ServerConstants.MAKE_FALSE);
					request.getSession().setAttribute(ServerConstants.USER_MASTER, boothAdminLogin1);
				}
			}
			if (result) {
				ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
				log.info("dwhjdqwhjdqjdg" + rootPaths);
				task.setDetails(deviceRegistration, messageSource, rootPaths);
				taskExecutor.execute(task);
				redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Transparent color saved");
				return ServerConstants.BOOTH_SETUP;
			}
		} catch (Exception e) {
			log.error("Error saveRGBValueBoothSetup", e);
			log.info("BoothAdminController Method:saveRGBValueBoothSetup");
		}
		return "redirect:cropEdges";
	}

	@RequestMapping(value = "cropEdges", method = RequestMethod.GET)
	public String boothSetUpByEventConfig(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		} else {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);

			Fovbyuser fovbyuser = boothAdminService.getFovTableData(boothAdminLogin1.getUserId());
			modelMap.addAttribute(ServerConstants.FOVBYUSER, fovbyuser);

			UploadImage uploadImage = boothAdminService.getCurrentImages(boothAdminLogin.getUserId());

			if (uploadImage.getImageName() != null) {
				modelMap.addAttribute(ServerConstants.UPLOAD_IMAGE, uploadImage);
			} else {
				modelMap.addAttribute("hide", "hide");
			}

			SignInVO signInVO1 = boothAdminService.getImageData(boothAdminLogin.getUserId());
			modelMap.addAttribute("signInVO1", signInVO1);
			return "cropEdges";
		}
	}

	@RequestMapping(value = "zoomScalePage")
	public String advanceBoothSetUpByEventConfig(@ModelAttribute("SignInVO") SignInVO signInVO,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		String result = null;
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin);
			UploadImage uploadImage = boothAdminService.getCurrentImages(boothAdminLogin.getUserId());
			if (uploadImage.getImageName() != null) {
				modelMap.addAttribute(ServerConstants.UPLOAD_IMAGE, uploadImage);
			} else {
				modelMap.addAttribute("hide", "hide");
			}
			if (signInVO != null) {
				signInVO.setUserId(boothAdminLogin.getUserId());
				result = boothAdminService.advanceBoothSetUpConfig(signInVO);
				modelMap.addAttribute("signInVO", signInVO);
			}
			if ((ServerConstants.SUCCESS).equals(result)) {
				Fovbyuser fovbyuser = boothAdminService.getFovTableData(boothAdminLogin1.getUserId());
				modelMap.addAttribute(ServerConstants.FOVBYUSER, fovbyuser);
				ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
				log.info("dwhjdqwhjdqjdg" + rootPaths);
				fovTask.setDetailsForFOV(deviceRegistration, messageSource, rootPaths);
				taskExecutor.execute(fovTask);
				return "redirect:cropEdges";
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "zoomPage")
	public String zoomPage(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			boothAdminLogin1 = boothAdminService.getProfileDetails(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN, boothAdminLogin);
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);
			UploadImage uploadImage = boothAdminService.getCurrentImages(boothAdminLogin.getUserId());
			if (uploadImage.getImageName() != null) {
				modelMap.addAttribute(ServerConstants.UPLOAD_IMAGE, uploadImage);
			} else {
				modelMap.addAttribute("hide", "hide");
			}
			Fovbyuser fovbyuser = boothAdminService.getFovTableData(boothAdminLogin1.getUserId());
			modelMap.addAttribute(ServerConstants.FOVBYUSER, fovbyuser);
			return "zoomScalePage";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "setZoomProfilePage", method = RequestMethod.POST)
	public String setZoomProfilePage(@ModelAttribute("SignInVO") SignInVO signInVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		String result;
		if (boothAdminLogin == null) {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		} else {
			result = boothAdminService.saveZoomScale(boothAdminLogin.getUserId(), signInVO);
			if (result.equals(ServerConstants.SUCCESS) && ("Save").equals(signInVO.getSave())) {
				return "redirect:zoomPage";
			}
		}
		return ServerConstants.BOOTH_SETUP;
	}

	@RequestMapping(value = "addImagesOfEvent")
	public String testing(@RequestParam(value = "eventId", required = ServerConstants.MAKE_TRUE) Integer eid,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null && eid != 0) {
			List<AdminPictureVO> adminPictureVOs2 = boothAdminService.getPicList(eid, boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.BOOTH_ADMIN_LOGIN2, boothAdminLogin1);
			modelMap.addAttribute("adminPictureVOs2", adminPictureVOs2);
			modelMap.addAttribute("eid", eid);
			Adminboothevent adminboothevent = boothAdminService.getEventDetails(eid);
			modelMap.addAttribute(ServerConstants.EVENT_NAME, adminboothevent.getEventName());
			List<ImageEmailFormVO> emailImagesList = boothAdminService
					.getPreSetBackGrounds(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.EMAIL_IMAGE_LIST, emailImagesList);
			Fovbyuser fovbyuser = boothAdminService.getFovTableData(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.FOVBYUSER, fovbyuser);

			List<AdminBoothEventPicture> notConfiguredImage = boothAdminService.notConfiguredImage(eid,
					boothAdminLogin.getUserId());
			modelMap.addAttribute("notConfiguredImage", notConfiguredImage.size());

			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			modelMap.addAttribute(ServerConstants.DEVICE_REGISTRATION, deviceRegistration);

			return "addEventImage";
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "addUploadBackgroundImage", method = RequestMethod.POST)
	public String addUploadBackgroundImage(
			@RequestParam(value = "files", required = ServerConstants.MAKE_FALSE) MultipartFile[] files,
			@ModelAttribute("AdminPictureVO") AdminPictureVO adminPictureVO, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes) throws IOException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		String rootPath = new java.io.File(request.getSession().getServletContext().getRealPath("") + "/..")
				.getCanonicalPath();
		if (boothAdminLogin != null && rootPath != null) {
			adminPictureVO.setCreatedBy(boothAdminLogin.getUserId());
			AdminPictureVO adminPictureVOs = boothAdminService.editUploadBackgroundImage(adminPictureVO, files,
					rootPath + ServerConstants.IMAGES);
			if (adminPictureVOs != null) {
				if (adminPictureVOs.getResult().equalsIgnoreCase(ServerConstants.SUCCESS)) {
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE,
							ServerConstants.UPLOAD_SUCCESS);
					return ServerConstants.ADD_IMAGES_OF_EVENT + adminPictureVOs.getEId();
				} else {
					redirectAttributes.addFlashAttribute(ServerConstants.ERROR_MESSAGE, ServerConstants.UPLOAD_FAILED);
					return ServerConstants.ADD_IMAGES_OF_EVENT + adminPictureVOs.getEId();
				}
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@RequestMapping(value = "delEventPicture")
	public String deletEventSinglePicture(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			RedirectAttributes redirectAttributes,
			@RequestParam(value = "picId", required = ServerConstants.MAKE_FALSE) String picId,
			@RequestParam(value = "eventId", required = ServerConstants.MAKE_FALSE) Integer eventId) {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
			String result = boothAdminService.deletEventSinglePicture(picId, eventId, request);
			if (result.equals(ServerConstants.SUCCESS)) {
				if (boothAdminLogin.getUserRole().equals("superadmin")) {
					ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
					taskImageUpdate.setDetails(deviceRegistration, messageSource, rootPaths);
					taskExecutor.execute(taskImageUpdate);
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Delete  image successfully");
					return "redirect:getUploadedImagesSA?eventId=" + eventId;
				} else {
					ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
					taskImageUpdate.setDetails(deviceRegistration, messageSource, rootPaths);
					taskExecutor.execute(taskImageUpdate);
					redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE, "Delete  image successfully");
					return ServerConstants.ADD_IMAGES_OF_EVENT + eventId;
				}
			} else {
				redirectAttributes.addFlashAttribute(ServerConstants.SUCCESS_MESSAGE,
						"Failed for deleting selecting image");
				return ServerConstants.ADD_IMAGES_OF_EVENT + eventId;
			}
		} else {
			return ServerConstants.REDIRECT_LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "publishNow")
	public String publishNow(@RequestParam(value = "eid", required = false) Integer eventId, HttpServletRequest request,
			HttpServletResponse response, ModelMap modelMap, RedirectAttributes redirectAttributes)
			throws ParseException {
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		if (boothAdminLogin != null) {
			if (boothAdminLogin.getUserRole().equals("superadmin")) {
				return "redirect:getSubscribedEventList";
			} else {
				String result = boothAdminService.updateEventDate(eventId);
				if (("success").equals(result)) {
					deviceRegistration = boothAdminService.getRegisteredDevice(boothAdminLogin.getUserId());
					if (!deviceRegistration.isEmpty()) {
						ThreadPoolTaskExecutor taskExecutor = pool.taskExecutor();
						taskImageUpdate.setDetails(deviceRegistration, messageSource, rootPaths);
						taskExecutor.execute(taskImageUpdate);
						//return "redirect:getSubscribedEventList";
						return "redirect:getUploadedImages?eventId="+eventId;
					} else {
						//return "redirect:getSubscribedEventList";
						return "redirect:getUploadedImages?eventId="+eventId;
					}
				}
			}
		}
		return ServerConstants.REDIRECT_LOGIN_PAGE;
	}

	@ResponseBody
	@RequestMapping(value = "validateTakeTestPicture", method = RequestMethod.GET)
	public boolean validateTakeTestPicture(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,
			@RequestParam(value = "userId", required = ServerConstants.MAKE_FALSE) Integer userId,
			RedirectAttributes redirectAttributes) {
		boolean results = ServerConstants.MAKE_FALSE;
		try {
			Fovbyuser fovbyuser = boothAdminService.getFovTableData(userId);

			if (fovbyuser.getImageWidth() != null) {
				results = ServerConstants.MAKE_TRUE;
			} else {
				results = ServerConstants.MAKE_FALSE;
			}
		} catch (Exception e) {
			log.info("BoothAdminController Method : sendTestMail");
			log.error("Error sendTestMail", e);
			e.getMessage();
		}
		return results;
	}

	@RequestMapping(value = "deleteProfileDetail")
	@ResponseBody
	public Boolean deleteUserProfile(HttpServletRequest request, HttpServletResponse response) {
		Boolean isDeleted = ServerConstants.MAKE_FALSE;
		boothAdminLogin = (BoothAdminLogin) request.getSession().getAttribute(ServerConstants.BOOTH_ADMIN_LOGIN);
		try {
			if (boothAdminLogin != null) {
				String rootPath = new java.io.File(
						request.getSession().getServletContext().getRealPath("") + "/.." + ServerConstants.IMAGES)
								.getCanonicalPath();
				isDeleted = boothAdminService.deleteUserProfile(boothAdminLogin.getUserId(), rootPath);
				// signUpPage();
			}
		} catch (Exception e) {
			log.info("BoothAdminController Method : deleteProfileDetail");
			log.error("Error deleteProfileDetail", e);
			e.getMessage();
		}

		return isDeleted;
	}
	
	@ResponseBody
	@RequestMapping(value = "updateSubscriptionFormAdmin", method = RequestMethod.POST, headers = "Accept=application/json")
	public BoothAdminLogin updateSubscriptionFormAdmin(@RequestBody UpdateSubscriptionReq updateSubscriptionReq, HttpServletRequest request) {
				return boothAdminService.updateSubscriptionFormAdmin(updateSubscriptionReq);
	}
	
	@RequestMapping(value = "accesstoken", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody TokenClsss token(@RequestBody TokenClsss token, HttpServletRequest request) {
		log.info("TokenClsss="+token);
		boothAdminService.addDataAccessToken(token);
		return token;
	}
	
	@RequestMapping(value = "removeaccesstoken", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody void RemoveToken(@RequestBody TokenClsss token, HttpServletRequest request) {
		boothAdminService.removeAccessToken(token);
		//return token;
	}

    

}
