function warningUtils(type, text) {
    $("#warningModalType").empty();
    $("#warningModalBody").empty();
    if(type=="Ok") {
        $("#warningModalBody").append('<img class="icons" src="../icons/ok.jpg" style="padding-left:15px;">');
    }
    else if(type=="Warning") {
        $("#warningModalBody").append('<img class="icons" src="../icons/warning.jpg" style="padding-left:15px;" >');
    }
    else {
        $("#warningModalBody").append('<img class="icons" src="../icons/error.jpg" style="padding-left:15px;" >');
    }

    $("#warningModalBody").append('<span style="padding-left:15px;font-size:15px">' + "&nbsp;" + text + '</span>');
    $("#warningModal").modal();
}

//divides the string "name" into substrings of at least "threshold" chars by locating the last space char (' ')
//returns an array of strings with each element containing a substring of the original string
function subdivideMetricName(name, threshold) {
    let res = [];
    res[0] = name;
    while (res[res.length-1].length > threshold) {
        let index = threshold;
        let aux = res[res.length-1]
        while (index >= 0){
            if(aux[index] === ' ') {
                res[res.length-1] = aux.substring(0, index);
                res.push(aux.substring(index+1, aux.length));
                break;
            }
            --index;
        }
        if(index < 0) {
            res[res.length-1] = res[res.length-1].substring(0, threshold);
            res.push(aux.substring(threshold, aux.length));
        }
    }
    return res;
}