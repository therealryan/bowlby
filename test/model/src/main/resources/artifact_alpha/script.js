document.addEventListener('DOMContentLoaded', function() {
  var p = document.createElement('p');
  p.innerHTML = 'With javascript!';
  document.getElementsByTagName('body').item(0).appendChild(p);
},false);
