package info.anecdot.tracking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author Stephan Grundner
 */
@Service
public class TrackingService {

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private TrackRepository trackRepository;

    private Visitor saveVisitor(Visitor visitor, boolean flush) {
        if (flush) {
            return visitorRepository.saveAndFlush(visitor);
        }

        return visitorRepository.save(visitor);
    }

    private synchronized Visitor findOrCreateVisitor(Cookie cookie) {
        String value = cookie.getValue();

        Visitor visitor = visitorRepository.findByCookieValue(value);
        if (visitor == null) {
            visitor = new Visitor();
            visitor.setCookieValue(value);
            visitor = saveVisitor(visitor, true);
        }

        return visitor;
    }

    public Track track(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = Stream.of(Optional.ofNullable(request.getCookies()).orElse(new Cookie[] {}))
                .filter(Objects::nonNull)
                .filter(it -> "VISITOR".equalsIgnoreCase(it.getName()))
                .findFirst().orElse(null);
        if (cookie == null) {
            cookie = new Cookie("VISITOR", UUID.randomUUID().toString());
            cookie.setMaxAge(Integer.MAX_VALUE);
            response.addCookie(cookie);
        }

        Visitor visitor = findOrCreateVisitor(cookie);
        request.setAttribute(Visitor.class.getName(), visitor);

        Track track = new Track();
        track.setVisitor(visitor);
        track.setUrl(request.getRequestURL().toString());

        String referer = (String) request.getAttribute(HttpHeaders.REFERER);
        track.setReferer(referer);

        String userAgent = request.getHeader("User-Agent");
        track.setUserAgent(userAgent);

        LocalDateTime moment = LocalDateTime.now();
        track.setMoment(moment);

        track = trackRepository.save(track);
        request.setAttribute(Track.class.getName(), track);

        return track;
    }
}
