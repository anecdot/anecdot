package info.anecdot.servlet;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Component
public class FunctionsExtension extends AbstractExtension {

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private class ItemsFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            HttpServletRequest request = currentRequest();

            SiteService siteService = applicationContext.getBean(SiteService.class);
            Site site = siteService.findSiteByRequest(request);

            EntityManager entityManager = applicationContext.getBean(EntityManager.class);

            Number limit = (Number) args.get("limit");
            if (limit == null) {
                limit = 255;
            }

            Number offset = (Number) args.get("offset");
            if (offset == null) {
                offset = 0;
            }

            StringBuilder ql = new StringBuilder();
            ql.append("select i from Item i where i.site = :site");

            String sort = (String) args.get("sort");
            if (StringUtils.hasText(sort)) {
                ql.append(" order by ")
                        .append(sort);
            }

            TypedQuery<Item> query = entityManager.createQuery(ql.toString(), Item.class);
            query.setParameter("site", site);

            List<Item> resultList = query
                    .setMaxResults(limit.intValue())
                    .setFirstResult(offset.intValue())
                    .getResultList();

            ItemService itemService = applicationContext.getBean(ItemService.class);
            return resultList.stream().map(itemService::toMap).collect(Collectors.toList());
        }

        @Override
        public List<String> getArgumentNames() {
            return Arrays.asList("path", "tags", "limit", "offset", "sort");
        }
    }

    private class EvalFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext evaluationContext, int lineNumber) {
            ExpressionParser expressionParser = new SpelExpressionParser();
            Expression expression = expressionParser.parseExpression(args.get("expr").toString());
            org.springframework.expression.EvaluationContext expressionEvaluationContext = new StandardEvaluationContext();
            Object model = evaluationContext.getVariable("page");
            ((StandardEvaluationContext) expressionEvaluationContext).setRootObject(model);
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            SiteService siteService = applicationContext.getBean(SiteService.class);
            ItemService itemService = applicationContext.getBean(ItemService.class);
            String host = servletRequestAttributes.getRequest().getServerName();
            Site site = siteService.findSiteByName(host);
            expressionEvaluationContext.setVariable("site", site);
            ((StandardEvaluationContext) expressionEvaluationContext).setBeanResolver(((context, beanName) -> {
                return applicationContext.getBean(beanName);
            }));

            return expression.getValue(expressionEvaluationContext);
        }

        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("expr");
        }
    }

    private class MarkdownFunction implements Function {

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext evaluationContext, int lineNumber) {
            String text = (String) args.get("text");
            if (StringUtils.hasLength(text)) {
                return HtmlRenderer.builder().build()
                        .render(Parser.builder().build()
                                .parse(text));
            }

            return text;
        }

        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("text");
        }
    }

    private final ApplicationContext applicationContext;

    @Override
    public Map<String, Function> getFunctions() {
        Map<String, Function> functions = new HashMap<>();
        functions.put("items", new ItemsFunction());
        functions.put("eval", new EvalFunction());
        functions.put("md", new MarkdownFunction());

        return functions;
    }

    public FunctionsExtension(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
