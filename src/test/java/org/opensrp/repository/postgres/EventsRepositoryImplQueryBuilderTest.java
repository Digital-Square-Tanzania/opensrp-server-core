package org.opensrp.repository.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.opensrp.domain.postgres.EventMetadataExample;
import org.opensrp.domain.postgres.EventMetadataExample.Criteria;
import org.opensrp.domain.postgres.EventMetadataExample.Criterion;
import org.opensrp.search.EventSearchBean;

public class EventsRepositoryImplQueryBuilderTest {

	@Test
	public void populateEventSearchCriteriaShouldSplitLocationAndTeamFiltersForTeamScopedEventTypes() throws Exception {
		EventsRepositoryImpl repository = new EventsRepositoryImpl();
		EventSearchBean eventSearchBean = new EventSearchBean();
		eventSearchBean.setLocationId("sync-location-id");
		eventSearchBean.setTeamId("sync-team-id");
		eventSearchBean.setEventType("teamId:Close Referral,LTFU Feedback");
		eventSearchBean.setServerVersion(25L);
		
		EventMetadataExample example = new EventMetadataExample();
		invokePopulateEventSearchCriteria(repository, eventSearchBean, example, false);
		
		assertEquals(2, example.getOredCriteria().size());
		
		Criteria locationCriteria = example.getOredCriteria().get(0);
		assertTrue(hasCriterion(locationCriteria, "location_id =", "sync-location-id"));
		assertTrue(hasCriterion(locationCriteria, "event_type not in",
		    Arrays.asList("Close Referral", "LTFU Feedback")));
		assertTrue(hasCriterion(locationCriteria, "server_version >=", 25L));
		assertFalse(hasCriterion(locationCriteria, "team_id =", "sync-team-id"));
		
		Criteria teamCriteria = example.getOredCriteria().get(1);
		assertTrue(hasCriterion(teamCriteria, "team_id =", "sync-team-id"));
		assertTrue(hasCriterion(teamCriteria, "event_type in", Arrays.asList("Close Referral", "LTFU Feedback")));
		assertTrue(hasCriterion(teamCriteria, "server_version >=", 25L));
		assertFalse(hasCriterion(teamCriteria, "location_id =", "sync-location-id"));
	}

	@Test
	public void populateEventSearchCriteriaShouldRetainStandardEventTypeFilteringWhenPrefixIsAbsent() throws Exception {
		EventsRepositoryImpl repository = new EventsRepositoryImpl();
		EventSearchBean eventSearchBean = new EventSearchBean();
		eventSearchBean.setLocationId("sync-location-id");
		eventSearchBean.setEventType("Close Referral,LTFU Feedback");
		
		EventMetadataExample example = new EventMetadataExample();
		invokePopulateEventSearchCriteria(repository, eventSearchBean, example, false);
		
		assertEquals(1, example.getOredCriteria().size());
		Criteria criteria = example.getOredCriteria().get(0);
		assertTrue(hasCriterion(criteria, "location_id =", "sync-location-id"));
		assertTrue(hasCriterion(criteria, "event_type in", Arrays.asList("Close Referral", "LTFU Feedback")));
	}

	private void invokePopulateEventSearchCriteria(EventsRepositoryImpl repository, EventSearchBean eventSearchBean,
	        EventMetadataExample example, boolean includeDetailedSearchFilters) throws Exception {
		Method method = EventsRepositoryImpl.class.getDeclaredMethod("populateEventSearchCriteria", EventSearchBean.class,
		    EventMetadataExample.class, boolean.class);
		method.setAccessible(true);
		method.invoke(repository, eventSearchBean, example, includeDetailedSearchFilters);
	}

	private boolean hasCriterion(Criteria criteria, String condition, Object value) {
		for (Criterion criterion : criteria.getAllCriteria()) {
			if (condition.equals(criterion.getCondition()) && value.equals(criterion.getValue())) {
				return true;
			}
		}
		return false;
	}
}
