var app = angular.module('TablesApp', []);
app.controller('TablesCtrl', function($scope, $http) {

    $scope.data = [];
    $scope.metricCategory = ["Default"];

    this.$onInit = function () {
        var url = "../api/metrics/list";
        $http({
            method: "GET",
            url: url
        }).then(function mySuccess(response) {
            $scope.metricCategory = response.data;
        })
    };

    $scope.getMetricsConfig = function() {
        console.log("IN getMetricsConfig");

        var url = "../api/metrics";
        $http({
            method: "GET",
            url: url
        }).then(function mySuccess(response) {
            var data = [];
            response.data.forEach(function (metric) {
                data.push({
                    id: metric.id,
                    externalId: metric.externalId,
                    name: metric.name,
                    description: metric.description,
                    threshold: metric.threshold,
                    webUrl: metric.webUrl,
                    metricCategory: metric.categoryName
                });
            });
            $scope.data = data;
            $scope.sortType = 'name';
            $scope.sortReverse = false;
        })
    };

    $scope.saveMetric = function(id, selectedCat) {
        var formData = new FormData();
        //formData.append("name", $('#QFName').val());
        //formData.append("description", $('#QFDescription').val());
        formData.append("threshold", document.getElementById("MetThreshold"+id).value);
        if(document.getElementById("MetUrl"+id).validity.valid) // if url input text is valid
            formData.append("url", document.getElementById("MetUrl"+id).value);
        formData.append("categoryName", selectedCat);

        $.ajax({
            url: "../api/metrics/"+id,
            data: formData,
            type: "PUT",
            contentType: false,
            processData: false,
            error: function(jqXHR, textStatus, errorThrown) {
                warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                location.href = "../Metrics/Configuration";
            },
            success: function() {
                location.href = "../Metrics/Configuration";
            }
        });
    }
});