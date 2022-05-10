/**
 * @version 0.1.1
 */
package com.upc.gessi.qrapids.app.config.security.seeds;

import com.upc.gessi.qrapids.app.domain.repositories.Route.RouteRepository;
import com.upc.gessi.qrapids.app.domain.repositories.UserGroup.UserGroupRepository;
import com.upc.gessi.qrapids.app.domain.models.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class RoutesFirstLoad {

    @Autowired
    RouteRepository routeRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    @PostConstruct
    public void init() throws Exception {

        // Set base dataset container
        List<Route> routes = new ArrayList<>();

        // has elements?
        if ( this.routeRepository.count() != 0 )
            return; // dons seed data

        // Application Route

        // REST - Strategic indicators

        // Changed to the routes the user can NOT acces
        routes.add( new Route( "REST : Strategic Indicators - Prediction Chart", "/StrategicIndicators/PredictionChart") );
        routes.add( new Route( "REST : Detailed Strategic Indicators - Prediction Chart", "/DetailedStrategicIndicators/PredictionChart") );
        routes.add( new Route( "REST : Quality Factors - Prediction Chart", "/QualityFactors/PredictionChart") );
        routes.add( new Route( "REST : Detailed Quality Factors - Prediction Chart", "/DetailedQualityFactors/PredictionChart") );
        routes.add( new Route( "REST : Metrics - Prediction Chart", "/Metrics/PredictionChart") );

        routes.add( new Route( "REST : Strategic Indicators - Current Evaluation", "/QualityRequirements") );

        routes.add( new Route( "REST : Decisions", "/Decisions") );

        routes.add( new Route( "REST : Reporting", "/Reporting") );

        routes.add( new Route( "REST : Phases", "/Phases") );

        routes.add( new Route( "REST : Products - Configuration", "/Products/Configuration") );
        routes.add( new Route( "REST : Strategic Indicators - Configuration", "/StrategicIndicators/Configuration") );
        routes.add( new Route( "REST : Quality Factors - Configuration", "/QualityFactors/Configuration") );
        routes.add( new Route( "REST : Metrics - Configuration", "/Metrics/Configuration") );
        routes.add( new Route( "REST : Metrics - Configuration", "/Metrics/Configuration#loaded") );
        routes.add( new Route( "REST : Categories - Configuration", "/Categories/Configuration") );
        routes.add( new Route( "REST : Profiles - Configuration", "/Profiles/Configuration") );
        routes.add( new Route( "REST : QRPatterns - Configuration", "/QRPatterns/Configuration") );
        routes.add( new Route( "REST : profile", "/profile") );
        routes.add( new Route( "REST : users", "/users") );


        /*
        routes.add( new Route( "REST : Strategic Indicators - Current Evaluation", "/CurrentEvaluation") );
        routes.add( new Route( "REST : Strategic Indicators - Historical Data", "/HistoricalData") );
        routes.add( new Route( "REST : Strategic Indicators - Detailed Strategic Indicators", "/DetailedStrategicIndicators") );

        // Strategic Indicators
        routes.add( new Route( "Strategic Indicators - Current Chart", "/StrategicIndicators/CurrentChart") );
        routes.add( new Route( "Strategic Indicators - Current Table", "/StrategicIndicators/CurrentTable") );

        routes.add( new Route( "Strategic Indicators - Historic Table", "/StrategicIndicators/HistoricTable") );
        routes.add( new Route( "Strategic Indicators - Historic Chart", "/StrategicIndicators/HistoricChart") );

        // Detailed Quality Factor
        routes.add( new Route( "Detailed Quality Factor - Current Chart", "/DetailedQualityFactor/CurrentChart") );
        routes.add( new Route( "Detailed Quality Factor - Current Stacked", "/DetailedQualityFactor/CurrentStacked") );
        routes.add( new Route( "Detailed Quality Factor - Current Polar", "/DetailedQualityFactor/CurrentPolar") );
        routes.add( new Route( "Detailed Quality Factor - Current Table", "/DetailedQualityFactor/CurrentTable") );

        routes.add( new Route( "Detailed Quality Factor - Historic Table", "/DetailedQualityFactor/HistoricTable") );
        routes.add( new Route( "Detailed Quality Factor - Historic Chart", "/DetailedQualityFactor/HistoricChart") );

        // Metrics
        routes.add( new Route( "Metrics - Current Table", "/Metrics/CurrentTable") );

        routes.add( new Route( "Metrics - Historic Table", "/Metrics/HistoricTable") );
        routes.add( new Route( "Metrics - Historic Chart", "/Metrics/HistoricChart") );

        // Detailed Strategic Indicators
        routes.add( new Route( "Detailed Strategic Indicators - Current Chart", "/DetailedStrategicIndicators/CurrentChart") );
        routes.add( new Route( "Detailed Strategic Indicators - Current Stacked", "/DetailedStrategicIndicators/CurrentStacked") );
        routes.add( new Route( "Detailed Strategic Indicators - Current Polar", "/DetailedStrategicIndicators/CurrentPolar") );
        routes.add( new Route( "Detailed Strategic Indicators - Current Bar", "/DetailedStrategicIndicators/CurrentBar") );
        routes.add( new Route( "Detailed Strategic Indicators - Current Table", "/DetailedStrategicIndicators/CurrentTable") );

        routes.add( new Route( "Detailed Strategic Indicators - Historic Table", "/DetailedStrategicIndicators/HistoricTable") );
        routes.add( new Route( "Detailed Strategic Indicators - Historic Chart", "/DetailedStrategicIndicators/HistoricChart") );

        // Administration Controller Routes
        routes.add( new Route( "Users Administration", "/users/*") );
        routes.add( new Route( "User Groups Administration", "/usergroups/*") );

        // User profile viewer and editable content
        routes.add( new Route( "User edit profile", "/profile/*", true) );*/

        for( Route route : routes ) {
            Route curr = this.routeRepository.findByName( route.getName() );
            if( curr == null )
                this.routeRepository.save( route );
        }

        /*  We can defina a new group using the follow lines.

            UserGroup userGroup = this.userGroupRepository.findName( "ADMIN" );

            if( userGroup == null ) {


                Set setRoutes = new HashSet<Route>( this.routeRepository.findAll() );
                userGroup = new UserGroup( );
                userGroup.setName("ADMIN");
                userGroup.setRoutes( setRoutes );

                this.userGroupRepository.save( userGroup );

            }
        */
    }
}
