define([
    'angular',
    'filters',
    'services',
    'directives',
    'controllers',
    'angular-route',
    'angular-animate',
    'angular-resource',
    'angular-cookies',
    'ui-bootstrap'
    ], function (angular, filters, services, directives, controllers) {
        'use strict';
        return angular.module('rhphd', ['ngRoute', 'ngAnimate', 'ngResource', 'ngCookies', 'rhphd.controllers', 'rhphd.filters', 'rhphd.services', 'rhphd.directives', 'ui.bootstrap']);
});
