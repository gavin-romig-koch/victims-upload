define(['angular', 'app'], function(angular, app) {
    'use strict';

    return app.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
        $routeProvider.when('/view1', {
            templateUrl: 'resources/partials/partial1.html',
            controller: 'MyCtrl1'
        });
        $routeProvider.when('/view2', {
            templateUrl: 'resources/partials/partial2.html',
            controller: 'MyCtrl2'
        });
        $routeProvider.when('/view3', {
            templateUrl: 'resources/partials/partial3.html',
            controller: 'MyCtrl3'
        });
        $routeProvider.when('/admin', {
            templateUrl: 'resources/partials/admin/index.html',
            controller: 'AdminController'
        });
        $routeProvider.when('/accounts/:accountId/orgs/:orgId/machines/:machineId', {
            templateUrl: 'resources/partials/main/account.html',
            controller: 'AccountController'
        });
        $routeProvider.when('/accounts/:accountId/orgs/:orgId', {
            templateUrl: 'resources/partials/main/account.html',
            controller: 'AccountController'
        });
        $routeProvider.when('/accounts/:accountId', {
            templateUrl: 'resources/partials/main/account.html',
            controller: 'AccountController'
        });
        $routeProvider.otherwise({redirectTo: '/'});
    }]);

});
