var currentColor = "#696969";
var metrics = [];
var metricsDB = [];
var factors = [];
var categories = [];


var profileId = sessionStorage.getItem("profile_id");

var id = false;
var urlLink;

const DEFAULT_CATEGORY = "Default";

var url;
if (getParameterByName('id').length !== 0) {
    id = true;
    url = parseURLComposed("../api/qualityFactors/metrics/current");
} else {
    var profileId = sessionStorage.getItem("profile_id");
    url = parseURLComposed("../api/metrics/current?profile="+profileId);
}

function getAllMetrics(){
    $.ajax({
        url : url,
        type: "GET",
        success: function (response) {
            if (id) // in case we show metrics for one detailed factor
                metrics = response[0].metrics;
            else // in case we show all metrics
                metrics = response;
            sortDataAlphabetically(metrics);
            jQuery.ajax({
                dataType: "json",
                url: "../api/metrics",
                cache: false,
                type: "GET",
                async: true,
                success: function (dataDB) {
                    metricsDB = dataDB;
                    getMetricsCategoriesAndShow();
                }});
        }
    });
}

function getFactors() {
    if (id)
        url = parseURLComposed("../api/qualityFactors/metrics/current");
    else
        url = "../api/qualityFactors/metrics/current?profile=" + profileId
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
            sortMyDataAlphabetically(data);
            factors = data;
            console.log("factors");
            console.log(factors);
            showMetricsSliders();
        }
    });
}

function sortMyDataAlphabetically (factors) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    factors.sort(compare);
}

function getMetricsCategoriesAndShow () {
    var url = "../api/metrics/categories";
    $.ajax({
        url : url,
        type: "GET",
        success: function (response) {
            categories = response;
            removeSpaces();
            getFactors();
        }
    });
}

function removeSpaces(){
    categories.forEach( function (cat) {
        cat.name = cat.name.replace(/ /g, '-')
        cat.type = cat.type.replace(/ /g, '-')
    })
}

function showMetricsSliders () {
    // Metrics categories
    let rangeHighlights = new Map;

    //obtaining each category name
    let categoryNames = [];
    for (let i = 0; i < categories.length; ++i) categoryNames.push(categories[i].name);
    categoryNames = new Set(categoryNames);

    categoryNames.forEach( function (cat) {
        let start = 0;
        let aux = [];
        let catList = categories.filter( function (elem) {
            return elem.name === cat;
        });
        catList.sort(function (a, b) {
            return a.upperThreshold - b.upperThreshold;
        });

        for (let i = 0; i < catList.length; i++) {
            let end = catList[i].upperThreshold;
            let offset = 0;
            if (end < 1) offset = 0.02;
            let range = {
                start: start,
                end: end + offset,
                class: catList[i].name + catList[i].type
            };
            aux.push(range);
            start = end;
        }
        rangeHighlights.set(cat, aux);
    });

    var metricsDiv = $("#metricsSliders");

    console.log(metrics);

    for (i = 0; i < factors.length; i++) {

        var divF = document.createElement('div');
        divF.style.marginTop = "1em";
        divF.style.marginBottom = "1em";

        var labelF = document.createElement('label');
        labelF.id = factors[i].id;
        labelF.textContent = factors[i].name;
        divF.appendChild(labelF);

        factors[i].metrics.forEach(function (metric) {
            var div = document.createElement('div');
            div.id = "div" + metric.id;
            div.style.marginTop = "1em";
            div.style.marginBottom = "1em";

            var label = document.createElement('label');
            label.id = metric.id;
            label.textContent = metric.name + " " + metric.value.toFixed(2);
            label.title = metric.description;
            label.style.marginLeft = "1em";

            var findMet = metricsDB.find(function (element) {
                return element.externalId === metric.id;
            });
            if (findMet) { // if metric not found it will be undefined
                urlLink = findMet.webUrl;
            }
            if (urlLink) {
                label.textContent = "";
                var a = document.createElement('a');
                a.href = urlLink;
                a.target = "_blank";
                a.textContent = metric.name + " " + metric.value.toFixed(2);
                label.appendChild(a);
            }

            var slider = document.createElement("input");
            slider.id = i + "sliderValue" + metric.id;
            slider.style.width = "60%";
            slider.style.height = "100%";
            var value = 0;
            if (metric.value !== 'NaN')
                value = metric.value;
            var sliderConfig = {
                id: "slider" + metric.id,
                min: 0,
                max: 1,
                step: 0.01,
                value: value,
                ticks: [value],
                lock_to_ticks: true,
                handle: 'triangle'
            };
            sliderConfig.rangeHighlights = [];

            let metricHighlights = rangeHighlights.get(DEFAULT_CATEGORY);
            if (findMet) metricHighlights = rangeHighlights.get(findMet.categoryName.replace(/ /g, '-'));

            Array.prototype.push.apply(sliderConfig.rangeHighlights, metricHighlights);
            div.appendChild(slider);
            div.appendChild(label);
            divF.append(div);
            metricsDiv.append(divF);
            $("#" + slider.id).slider(sliderConfig);
        });
    }
    // Add metrics without factor
    var divNOF = document.createElement('div');
    divNOF.style.marginTop = "1em";
    divNOF.style.marginBottom = "1em";

    var labelNOF = document.createElement('label');
    labelNOF.id = "withoutfactor";
    labelNOF.textContent = "Metrics not associated to any factor";
    divNOF.appendChild(labelNOF);
    metrics.forEach(function (metric) {
        var mdiv = document.getElementById("div"+metric.id);
        if (!mdiv) {
            console.log(mdiv);
            var div = document.createElement('div');
            div.id = "div" + metric.id;
            div.style.marginTop = "1em";
            div.style.marginBottom = "1em";

            var label = document.createElement('label');
            label.id = metric.id;
            label.textContent = metric.name + " " + metric.value.toFixed(2);
            label.title = metric.description;
            label.style.marginLeft = "1em";

            var findMet = metricsDB.find(function (element) {
                return element.externalId === metric.id;
            });
            if (findMet) { // if metric not found it will be undefined
                urlLink = findMet.webUrl;
            }
            if (urlLink) {
                label.textContent = "";
                var a = document.createElement('a');
                a.href = urlLink;
                a.target = "_blank";
                a.textContent = metric.name + " " + metric.value.toFixed(2);
                label.appendChild(a);
            }

            var slider = document.createElement("input");
            slider.id = "sliderValue" + metric.id;
            slider.style.width = "60%";
            slider.style.height = "100%";
            var value = 0;
            if (metric.value !== 'NaN')
                value = metric.value;
            var sliderConfig = {
                id: "slider" + metric.id,
                min: 0,
                max: 1,
                step: 0.01,
                value: value,
                ticks: [value],
                lock_to_ticks: true,
                handle: 'triangle'
            };
            sliderConfig.rangeHighlights = [];

            let metricHighlights = rangeHighlights.get(DEFAULT_CATEGORY);
            if (findMet) metricHighlights = rangeHighlights.get(findMet.categoryName.replace(/ /g, '-'));

            Array.prototype.push.apply(sliderConfig.rangeHighlights, metricHighlights);
            div.appendChild(slider);
            div.appendChild(label);
            divNOF.append(div);
            metricsDiv.append(divNOF);
            $("#"+slider.id).slider(sliderConfig);
        }
    });
    for (var j = 0; j < categories.length; j++) {
        $(".slider-rangeHighlight." + categories[j].name + categories[j].type).css("background", categories[j].color)
    }
}

function sortDataAlphabetically (metrics) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    metrics.sort(compare);
}

window.onload = function() {
    getAllMetrics();
}