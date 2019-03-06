package info.anecdot.servlet;

import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Stephan Grundner
 */
@Component
public class ItemHandler implements HttpRequestHandler {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ViewResolver viewResolver;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ModelAndView modelAndView = new ModelAndView();
        Item item = (Item) request.getAttribute(Item.class.getName());
        modelAndView.addObject("page", itemService.toMap(item));
        modelAndView.setViewName(item.getType());
        try {
            View view = viewResolver.resolveViewName(modelAndView.getViewName(), LocaleContextHolder.getLocale());
            view.render(modelAndView.getModel(), request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
