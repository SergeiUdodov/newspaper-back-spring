package com.nic.newspaper.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nic.newspaper.dao.ArticleDao;
import com.nic.newspaper.entity.Article;
import com.nic.newspaper.entity.Comment;
import com.nic.newspaper.entity.Theme;
import com.nic.newspaper.entity.User;
import com.nic.newspaper.model.CrmArticle;
import com.nic.newspaper.service.ArticleService;
import com.nic.newspaper.service.CommentService;
import com.nic.newspaper.service.ThemeService;
import com.nic.newspaper.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class ArticleServiceImpl implements ArticleService {

	@Autowired
	public ArticleDao articleDao;
	
	@Autowired
	private CommentService commentService;

	@Autowired
	private UserService userService;

	@Autowired
	private ThemeService themeService;

	SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	@Override
	@Transactional
	public List<Article> findAll(HttpServletRequest request) {

		List<Article> articles = articleDao.findAll();
		articles.forEach(article -> article.setFormattedDate(formatter.format(article.getDate())));
		
		//sorting articles by publication date
		Comparator<Article> byDate = (first, second) -> second.getDate().compareTo(first.getDate());

		articles.sort(byDate);

		String requestTokenHeader = request.getHeader("Authorization");

		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			
			//retrieving current logged user from db
			User currentUser = userService.getUserByToken(request);
			
			if(currentUser == null) {
				throw new RuntimeException("User not found");
			}

			List<Article> articlesForRemove = new ArrayList<>();
			
			List<Theme> forbidenThemes = currentUser.getForbid();

			//checking if any forbidden theme of current user contains in any article
			//and preparing for deleting such articles from initial list of articles
			for (Article article : articles) {
				if (!Collections.disjoint(article.getThemes(), forbidenThemes)) {
					articlesForRemove.add(article);
				}
			}

			//deleting articles with forbidden from initial list of articles
			articles.removeAll(articlesForRemove);

			List<Theme> preferredThemes = currentUser.getPrefer();
			
			//sorting articles by preferred themes
			Comparator<Article> byPreferredThemes = (first, second) -> checkThemesIntersection(second, preferredThemes)
					- checkThemesIntersection(first, preferredThemes);
			articles.sort(byPreferredThemes);
		}
		
		//filter to get articles published less than 24 hours ago
		long currentTimeMilliseconds = new Date().getTime();
		long oneDayMilliseconds = 24 * 60 * 60 * 1000;
		articles = articles.stream().filter(article -> currentTimeMilliseconds - article.getDate().getTime() < oneDayMilliseconds).collect(Collectors.toList());
		
		return articles;
	}
	
		private int checkThemesIntersection(Article article, List<Theme> list) {
			int result = 0;
			List<Theme> articleThemes = article.getThemes();
			
			if(articleThemes == null) {
				return result;
			}
			
			for (Theme theme : list) {
				if (articleThemes.contains(theme)) {
					result++;
				}
			}
			return result;
		}

	@Override
	@Transactional
	public Article save(CrmArticle theArticle) {

		Article newArticle = new Article();

		newArticle.setHeader(theArticle.getHeader());
		newArticle.setContent(theArticle.getContent());
		newArticle.setDate(new Date());
		newArticle.setImageURL(theArticle.getImageURL());

		List<Theme> themes = extractThemes(theArticle);
		newArticle.setThemes(themes);

		articleDao.save(newArticle);
		
		return newArticle;
	}
	
	@Override
	@Transactional
	public Article update(long articleId, CrmArticle theArticle) {

		Article newArticle = findArticleById(articleId);

		if (newArticle == null) {
			throw new RuntimeException("Article id not found - " + articleId);
		}

		newArticle.setHeader(theArticle.getHeader());
		newArticle.setContent(theArticle.getContent());
		newArticle.setDate(new Date());
		newArticle.setImageURL(theArticle.getImageURL());

		List<Theme> themes = extractThemes(theArticle);
		newArticle.setThemes(themes);

		articleDao.update(newArticle);
		
		return newArticle;
	}
	
		private List<Theme> extractThemes(CrmArticle theArticle) {
		//getting themes specified during creation or updating article
		String[] themesNames = theArticle.getThemes().toLowerCase().split("[^a-zа-я0-9]+");
		//removing repeating themes
		String[] uniqueNames = Arrays.stream(themesNames).distinct().toArray(String[]::new);

		List<Theme> themes = new ArrayList<>();

		//adding themes into final list
		Theme temp = null;
		for (String themeName : uniqueNames) {
			if (!"".equals(themeName)) {
				temp = themeService.findThemeByName(themeName);
				//if theme already exists in db then just add it into final list
				if (temp != null) {
					themes.add(temp);
				//if not, then at first add such theme into db, and then add it into final list
				} else {
					themeService.save(new Theme(themeName));
					temp = themeService.findThemeByName(themeName);

					themes.add(temp);
				}
			}
		}
		temp = null;
		return themes;
	}

	@Override
	@Transactional
	public Article findArticleById(long articleId) {

		return articleDao.findArticleById(articleId);
	}

	@Override
	@Transactional
	public void deleteArticleById(long articleId) {
		
		Article tempArticle = findArticleById(articleId);

		if (tempArticle == null) {
			throw new RuntimeException("Article id not found - " + articleId);
		}

		List<Comment> comments = tempArticle.getComments();

		if (comments != null) {

			for (Comment comment : comments) {
				commentService.deleteCommentById(comment.getId());
			}
		}

		articleDao.deleteArticleById(articleId);
	}

	@Override
	@Transactional
	public Article likeArticle(long articleId, HttpServletRequest request) {

		User currentUser = userService.getUserByToken(request);

		Article theArticle = findArticleById(articleId);

		if (theArticle == null) {
			throw new RuntimeException("Article id not found - " + articleId);
		}
		
		//getting list of users that liked this article
		List<User> likes = theArticle.getLikes();
		
		//every time user clicks like on this article we add and remove him from list of likers consistently
		if (likes.contains(currentUser)) {
			likes.remove(currentUser);
		} else {
			likes.add(currentUser);
		}

		theArticle.setLikes(likes);

		articleDao.update(theArticle);
		
		return theArticle;
	}

}
