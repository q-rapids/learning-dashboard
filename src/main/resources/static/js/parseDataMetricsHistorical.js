var isSi = false;
var isdsi = false;
var isqf = false;
var isdqf = false;

var url;
if (getParameterByName('id').length !== 0) {
    url = parseURLComposed("../api/qualityFactors/metrics/historical");
} else {
    var profileId = sessionStorage.getItem("profile_id");
    url = parseURLComposed("../api/metrics/historical?profile="+profileId);
}

var groupByFactor;
if(Boolean(sessionStorage.getItem("groupByFactor")) === false) {
    sessionStorage.setItem("groupByFactor", "true");
    groupByFactor = true;
} else {
    groupByFactor = sessionStorage.getItem("groupByFactor") === "true";
}
let checkbox = document.getElementById("groupByFactorCheckbox");
checkbox.checked = groupByFactor;

//initialize data vectors
var texts = [];
var ids = [];
var value = [];
var labels = [];
var categories = [];
var metricsDB = [];
let factors = [];
var orderedMetricsDB = [];
var decisions = new Map();

function clickCheckbox(){
    var checkbox = document.getElementById("groupByFactorCheckbox");
    sessionStorage.setItem("groupByFactor", checkbox.checked.toString());
    location.href = serverUrl + "/Metrics/HistoricChart";
}

function getData() {
    getDecisions();
    getMetricsDB();
    getFactors();
    texts = [];
    ids = [];
    value = [];
    labels = [];
    //get data from API
    jQuery.ajax({
        dataType: "json",
        url: url,
        data: {
            "from": $('#datepickerFrom').val(),
            "to": $('#datepickerTo').val()
        },
        cache: false,
        type: "GET",
        async: true,
        success: function (response) {
            var data = response;
            console.log("MY Data");
            console.log(data);
            if (getParameterByName('id').length !== 0) {
                data = response[0].metrics;
            }

            if(!groupByFactor) sortDataAlphabetically(data);
            else data = sortDataByFactor(data);

            j = 0;
            var line = [];
            var decisionsAdd = [];
            var decisionsIgnore = [];
            if (data[j]) {
                last = data[j].id;
                texts.push(data[j].name);
                ids.push(data[j].id);
                labels.push([data[j].name]);
            }
            while (data[j]) {
                //check if we are still on the same metric
                if (data[j].id != last) {
                    var val = [line];
                    if (decisionsAdd.length > 0) {
                        val.push(decisionsAdd);
                    }
                    if (decisionsIgnore.length > 0) {
                        val.push(decisionsIgnore);
                    }
                    value.push(val);
                    line = [];
                    decisionsAdd = [];
                    decisionsIgnore = [];
                    last = data[j].id;
                    texts.push(data[j].name);
                    ids.push(data[j].id);
                    var labelsForOneChart = [];
                    labelsForOneChart.push(data[j].name);
                    if (decisions.has(data[j].id)) {
                        var metricDecisions = decisions.get(data[j].id);
                        for (var i = 0; i < metricDecisions.length; i++) {
                            if (metricDecisions[i].type === "ADD") {
                                decisionsAdd.push({
                                    x: metricDecisions[i].date,
                                    y: 1.1,
                                    requirement: metricDecisions[i].requirement,
                                    comments: metricDecisions[i].comments
                                });
                            }
                            else {
                                decisionsIgnore.push({
                                    x: metricDecisions[i].date,
                                    y: 1.2,
                                    requirement: metricDecisions[i].requirement,
                                    comments: metricDecisions[i].comments
                                });
                            }
                        }
                        if (decisionsAdd.length > 0)
                            labelsForOneChart.push("Added decisions");
                        if (decisionsIgnore.length > 0)
                            labelsForOneChart.push("Ignored decisions");
                    }
                    labels.push(labelsForOneChart);
                }
                //push date and value to line vector
                if (!isNaN(data[j].value)) {
                    line.push({
                        x: data[j].date,
                        y: data[j].value
                    });
                }
                ++j;
            }
            //push line vector to values vector for the last metric
            if (data[j - 1]) {
                var val = [line];
                if (decisionsAdd.length > 0)
                    val.push(decisionsAdd);
                if (decisionsIgnore.length > 0)
                    val.push(decisionsIgnore);
                value.push(val);
            }
            sortMetricsDB();
            getMetricsCategories();
        }
    });
}

function sortMetricsDB () {
    ids.forEach( function (id) {
        orderedMetricsDB.push(
            metricsDB.find(elem => elem.externalId === id)
        )
    })
}

function sortDataAlphabetically (data) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    data.sort(compare);
}

function sortDataByFactor(data) {
    if(data.length === 0) return data;
    let resultData = []
    let writtenIds = new Set;
    for(let i = 0; i < factors.length; ++i){
        for(let j = 0; j < factors[i].metrics.length; ++j){
            let elems = data.filter( e => e.id === factors[i].metrics[j].id);
            resultData = resultData.concat(elems);
            writtenIds.add(elems[0].id);
        }
    }
    writtenIds = Array.from(writtenIds);
    let remainingMetrics = data.filter(x => !writtenIds.includes(x.id));
    return resultData.concat(remainingMetrics);
}

function getMetricsCategories () {
    jQuery.ajax({
        url: "../api/metrics/categories",
        type: "GET",
        async: true,
        success: function (response) {
            categories = response;
            drawChart();
        }
    });
}

function getMetricsDB() {
    jQuery.ajax({
        dataType: "json",
        url: "../api/metrics",
        cache: false,
        type: "GET",
        async: false,
        success: function (dataDB) {
            metricsDB = dataDB;
        }
    });
}

function getFactors() {
    let factorUrl;
    if (getParameterByName('id').length !== 0)
        factorUrl = parseURLComposed("../api/qualityFactors/metrics/current");
    else
        factorUrl = "../api/qualityFactors/metrics/current?profile=" + profileId
    jQuery.ajax({
        dataType: "json",
        url: factorUrl,
        cache: false,
        type: "GET",
        async: false,
        success: function (dataF) {
            sortDataAlphabetically(dataF);
            factors = dataF;
        }
    });
}

window.onload = function() {
    getData();
};