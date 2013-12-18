define(['angular', 'services'], function (angular) {
    'use strict';

    // injector method takes an array of modules as the first argument
    // if you want your controller to be able to use components from
    // any of your other modules, make sure you include it together with 'ng'
    // Furthermore we need to pass on the $scope as it's unique to this controller
    return angular.module('rhphd.controllers', ['rhphd.services'])
        .controller('MyCtrl1', ['$scope', 'version', function ($scope, version) {
            $scope.scopedAppVersion = version;
        }])
        // More involved example where controller is required from an external file
        .controller('MyCtrl2', ['$scope', '$injector', function($scope, $injector) {
            require(['controllers/myctrl2'], function(myctrl2) {
                $injector.invoke(myctrl2, this, {'$scope': $scope});
            });
        }])
        .controller('AccountController', ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/AccountController'], function(AccountController) {
                $injector.invoke(AccountController, this, {'$scope': $scope});
            });
        }])
        .controller('MachineController', ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/MachineController'], function(MachineController) {
                $injector.invoke(MachineController, this, {'$scope': $scope});
            });
        }])
        .controller('AdminController', ['$scope', '$injector', function ($scope, $injector) {
            require(['controllers/AdminController'], function(AdminController) {
                $injector.invoke(AdminController, this, {'$scope': $scope});
            });
        }])
});
