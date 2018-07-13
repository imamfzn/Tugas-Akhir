const maxP = 8;
const maxQ = 8;
let i = 0;

for (let p = 0 ; p <= maxP ; p++){
	for (let q = 0 ; q <= maxQ ; q++){
		if (p !== 0 || q !== 0){
			console.log(++i, p, q);
		}
	}
}