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

//initialize data vectors
var texts = [];
var ids = [];
var value = [];
var labels = [];
var categories = [];
var metricsDB = [];
let factors = [];
let students = [];
var orderedMetricsDB = [];
var decisions = new Map();
let rationales = [];

let printMetrics = true;
let global = false;

var groupByFactor = sessionStorage.getItem("groupByFactor") === "true";
var groupByStudent = sessionStorage.getItem("groupByStudent") === "true";
var groupByTeam = sessionStorage.getItem("groupByTeam") === "true";

function setUpGroupSelector(global){

    if(Boolean(sessionStorage.getItem("groupByFactor")) === false || (global && groupByStudent) || (!global && groupByTeam)) {
        //if cookies are not initialized, or is trying to group by student from a global project, or is trying to group by team from a non-global project
        sessionStorage.setItem("groupByFactor", "true");
        sessionStorage.setItem("groupByStudent", "false");
        sessionStorage.setItem("groupByTeam", "false");
    }

    groupByFactor = sessionStorage.getItem("groupByFactor") === "true";
    groupByStudent = sessionStorage.getItem("groupByStudent") === "true";
    groupByTeam = sessionStorage.getItem("groupByTeam") === "true";

    $("#selectorDropdownItems").append('<li><a onclick="clickFactorSelector()" href="#"> Group by factor </a></li>');
    if(global){
        $("#selectorDropdownItems").append('<li><a onclick="clickTeamSelector()" href="#"> Group by team </a></li>');
    }else{
        $("#selectorDropdownItems").append('<li><a onclick="clickStudentSelector()" href="#"> Group by student </a></li>');
    }

    if(groupByTeam) $("#selectorDropdownText").text('Group by team');
    else if(groupByStudent) $("#selectorDropdownText").text('Group by student');
    else $("#selectorDropdownText").text('Group by factor');
}

function clickFactorSelector(){
    sessionStorage.setItem("groupByFactor", "true");
    sessionStorage.setItem("groupByStudent", "false");
    sessionStorage.setItem("groupByTeam", "false");

    $("#selectorDropdownText").text('Group by factor');

    groupByFactor = true;
    groupByStudent = false;
    groupByTeam = false;

    location.href = serverUrl + "/Metrics/HistoricChart";
}

function clickStudentSelector() {
    sessionStorage.setItem("groupByFactor", "false");
    sessionStorage.setItem("groupByStudent", "true");
    sessionStorage.setItem("groupByTeam", "false");

    $("#selectorDropdownText").text('Group by student');

    groupByFactor = false;
    groupByStudent = true;
    groupByTeam = false;

    location.href = serverUrl + "/Metrics/HistoricChart";
}

function clickTeamSelector() {
    sessionStorage.setItem("groupByFactor", "false");
    sessionStorage.setItem("groupByStudent", "false");
    sessionStorage.setItem("groupByTeam", "true");

    $("#selectorDropdownText").text('Group by team');

    groupByFactor = false;
    groupByStudent = false;
    groupByTeam = true;

    location.href = serverUrl + "/Metrics/HistoricChart";
}

function getCurrentProjects() {
    var urlp = "/api/projects";
    jQuery.ajax({
        dataType: "json",
        url: urlp,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
            for(var i=0; i<data.length; i++) {
                if(data[i].name===sessionStorage.getItem("prj")) {
                    global = data[i].isGlobal;
                    setUpGroupSelector(global)
                }
            }
        }
    });
}

function getDatabyFactor() {
    getDecisions();
    getMetricsDB();
    getFactors();

    texts = [];
    ids = [];
    value = [];
    labels = [];
    rationales = [];
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
            if(groupByFactor || groupByTeam) data = sortDataByFactor(data);
            else sortDataAlphabetically(data);
            j = 0;
            var line = [];
            let rationaleLine = [];
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

                    let rat = [rationaleLine]
                    rationales.push(rat)
                    rationaleLine = [];

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
                    rationaleLine.push(data[j].rationale)
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

                let rat = [rationaleLine];
                rationales.push(rat);
            }
            sortMetricsDB();
            getMetricsCategories();
        }
    });
}

function getDataStudents() {


    getDecisions();
    getMetricsDB();

    texts = [];
    ids = [];
    value = [];
    labels = [];
    rationales = [];
    var prj = sessionStorage.getItem("prj")
    //get data from API
    jQuery.ajax({
        dataType: "json",
        url: "../api/metrics/student/historical",
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
            var i=0
            if(response.length===0) warningUtils("Warning", "This project has no students. Go to products &#x2192 project &#x2192 project team members")
            while (i<response.length) {
                students.push([response[i].studentName, response[i].numberMetrics])
                data = response[i].metrics
                j = 0;
                var line = [];
                let rationaleLine = [];
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

                        let rat = [rationaleLine]
                        rationales.push(rat)
                        rationaleLine = [];

                        decisionsAdd = [];
                        decisionsIgnore = [];
                        last = data[j].id;
                        texts.push(data[j].id);
                        ids.push(data[j].id);
                        var labelsForOneChart = [];
                        labelsForOneChart.push(data[j].id);
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
                                } else {
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
                        rationaleLine.push(data[j].rationale)
                    }
                    ++j;
                }
                if (data[j - 1]) {
                    var val = [line];
                    if (decisionsAdd.length > 0)
                        val.push(decisionsAdd);
                    if (decisionsIgnore.length > 0)
                        val.push(decisionsIgnore);
                    value.push(val);

                    let rat = [rationaleLine];
                    rationales.push(rat);
                }
                ++i;
            }
            //push line vector to values vector for the last metric

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
    let resultData = []
    let writtenIds = new Set;
    for(let i = 0; i < factors.length; ++i){
        for(let j = 0; j < factors[i].metrics.length; ++j){
            let elems = data.filter( e => e.id === factors[i].metrics[j].id);
            resultData = resultData.concat(elems);
            if (elems[0])
                writtenIds.add(elems[0].id);
        }
    }
    writtenIds = Array.from(writtenIds);
    if(global) {
        return resultData;
    } else {
        //adding metrics without factor
        let remainingMetrics = data.filter(x => !writtenIds.includes(x.id));
        return resultData.concat(remainingMetrics);
    }
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
            if (global){
                factors = filterGlobalFactor(dataF);
                console.log("factors");
                console.log(factors);
                if(factors.length === 0) {
                    if(groupByFactor) warningUtils("Warning", "No factors found. Add a new quality factor to view this data.")
                    else if(groupByTeam) warningUtils("Warning", "No teams found. Add a new quality factor starning with \"Team\" to view this data.")
                    else warningUtils("Warning", "No factors found.")
                }
            } else {
                sortFactors(dataF);
                factors = dataF;
                console.log("factors");
                console.log(factors);
            }
        }
    });
}

function filterGlobalFactor (factors) {
    if(groupByTeam) {
        factors = factors.filter( f => f.name.includes("Team"))
        sortFactors(factors)
    } else {
        factors = factors.filter( f => !f.name.includes("Team"))
        sortDataAlphabetically(factors)
    }
    return factors;
}

function sortFactors (factors) {
    function compare(a, b){
        if (a.type === b.type) return a.name > b.name ? 1 : -1;
        else return a.type !== "Github" ? 1 : -1;
    }
    factors.sort(compare);
}

function getData () {
    if(!groupByStudent) getDatabyFactor();
    else getDataStudents();
}

window.onload = function() {
    getCurrentProjects();
    getData();
};