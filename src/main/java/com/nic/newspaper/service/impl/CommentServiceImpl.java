package com.nic.newspaper.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nic.newspaper.dao.ArticleDao;
import com.nic.newspaper.dao.CommentDao;
import com.nic.newspaper.entity.Article;
import com.nic.newspaper.entity.Comment;
import com.nic.newspaper.entity.User;
import com.nic.newspaper.model.CrmComment;
import com.nic.newspaper.service.ArticleService;
import com.nic.newspaper.service.CommentService;
import com.nic.newspaper.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	private ArticleService articleService;
	
	@Autowired
	private ArticleDao articleDao;

	@Autowired
	private CommentService commentService;

	@Autowired
	private CommentDao commentDao;
	
	@Autowired
	private UserService userService;
	
	protected final Log logger = LogFactory.getLog(getClass());

	SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	@Override
	@Transactional
	public Article save(long articleId, CrmComment theComment, HttpServletRequest request) {

		Article theArticle = articleService.findArticleById(articleId);

		if (theArticle == null) {
			throw new RuntimeException("Article id not found - " + articleId);
		}

		User theUser = userService.getUserByToken(request);

		if (theUser == null) {
			throw new RuntimeException("User not found");
		}

		Comment newComment = new Comment();

		newComment.setText(theComment.getText());
		newComment.setDate(new Date());
		newComment.setUser(theUser);

		List<Comment> comments = theArticle.getComments();
		comments.add(newComment);
		theArticle.setComments(comments);

		articleDao.update(theArticle);
		
		return theArticle;
	}

	@Override
	@Transactional
	public void deleteCommentById(long commentId) {
		
		Comment tempComment = commentService.findCommentById(commentId);

		if (tempComment == null) {
			throw new RuntimeException("Comment id not found - " + commentId);
		}

		commentDao.deleteCommentById(commentId);
	}

	@Override
	@Transactional
	public Comment findCommentById(long commentId) {

		return commentDao.findCommentById(commentId);
	}

	@Override
	public List<Comment> articleComments(long aritcleId) {
		
		Article article = articleService.findArticleById(aritcleId);
		if(article == null) {
			return null;
		}
		
		List<Comment> comments = article.getComments();
		if(comments == null) {
			return null;
		}

		//sorting articles by publication date
		comments.forEach(comment -> comment.setFormattedDate(formatter.format(comment.getDate())));
		Comparator<Comment> byDate = (first, second) -> second.getDate().compareTo(first.getDate());

		comments.sort(byDate);

		return comments;
	}
}
