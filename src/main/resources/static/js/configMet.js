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

    $scope.saveMetric = function(id) {
        var formData = new FormData();
        //formData.append("name", $('#QFName').val());
        //formData.append("description", $('#QFDescription').val());
        formData.append("threshold", document.getElementById("MetThreshold"+id).value);
        if(document.getElementById("MetUrl"+id).validity.valid) // if url input text is valid
            formData.append("url", document.getElementById("MetUrl"+id).value);
        else {
            warningUtils("Error", "URL not valid");
            return;
        }
        formData.append("categoryName", document.getElementById("MetCategory"+id).value.replace('string:', ''));

        $.ajax({
            url: "../api/metrics/"+id,
            data: formData,
            type: "PUT",
            contentType: false,
            processData: false,
            error: function(jqXHR, textStatus, errorThrown) {
                warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
            },
            success: function() {
                warningUtils("Ok", "Metric saved successfully")
            }
        });
    }

    $scope.saveAllMetrics = function () {
        let cont = 0;
        $scope.data.forEach( function (elem) {
            console.log($scope.data);
            let id = elem.id;
            if (document.getElementById("MetCategory"+id) !== null) {
                let threshold = document.getElementById("MetThreshold" + id).value;
                let url = document.getElementById("MetUrl" + id).validity.valid ? document.getElementById("MetUrl" + id).value : '';
                let categoryName = document.getElementById("MetCategory" + id).value.replace('string:', '');

                if (elem.metricCategory !== categoryName || ((elem.threshold === null ? '' : elem.threshold) !== threshold) ||
                    (url !== null && elem.webUrl !== url)) { //url not null and different
                    let formData = new FormData();
                    //formData.append("name", $('#QFName').val());
                    //formData.append("description", $('#QFDescription').val());
                    formData.append("threshold", threshold);
                    formData.append("url", url);
                    formData.append("categoryName", categoryName);
                    ++cont;
                    $.ajax({
                        url: "../api/metrics/" + id,
                        data: formData,
                        type: "PUT",
                        contentType: false,
                        processData: false,
                        error: function (jqXHR, textStatus, errorThrown) {
                            warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                            --cont;
                        },
                        success: function () {
                            console.log('Updated element with id: ' + id)
                            if(--cont === 0) {
                                $scope.getMetricsConfig();
                                warningUtils("Ok", "Metrics saved successfully")
                            }
                        }
                    });
                }
            }
        })
    }
});