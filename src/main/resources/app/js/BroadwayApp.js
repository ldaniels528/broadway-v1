/**
 * Broadway Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
(function () {
    var app = angular.module('broadway', ['ngResource', 'ui.bootstrap']);
    app.config(['$resourceProvider', function ($resourceProvider) {
        // Don't strip trailing slashes from calculated URLs
        $resourceProvider.defaults.stripTrailingSlashes = false;
    }]);

    app.run(function ($rootScope) {
        $rootScope.version = "0.8.0";

        /******************************************************************
         *  Tab-related Methods
         ******************************************************************/

        $rootScope.tabs = [
            {
                "name": "Narratives",
                "contentURL" : "/app/views/narratives.htm",
                "imageURL": "/app/images/tabs/main/inspect-24.png",
                "active": false
            }, {
                "name": "Processes",
                "contentURL" : "/app/views/processes.htm",
                "imageURL": "/app/images/tabs/main/observe-24.png",
                "active": false
            }
        ];

        // select the default tab and make it active
        $rootScope.tab = $rootScope.tabs[0];
        $rootScope.tab.active = true;

        /**
         * Changes the active tab
         * @param index the given tab index
         * @param event the given click event
         */
        $rootScope.changeTab = function (index, event) {
            // deactivate the current tab
            $rootScope.tab.active = false;

            // activate the new tab
            $rootScope.tab = $rootScope.tabs[index];
            $rootScope.tab.active = true;

            if (event) {
                event.preventDefault();
            }
        };

    });

})();