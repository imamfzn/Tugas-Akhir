id_exp=$1
identifier=1
best_finder=1
builder=1

case $id_exp in
    1)
        best_finder=1
        builder=10
    ;;
    2)
        best_finder=2
        builder=5
    ;;
    3)
        best_finder=2
        builder=10
    ;;
    4)
        best_finder=5
        builder=5
    ;;
    5)
        best_finder=5
        builder=10
    ;;
    6)
        best_finder=10
        builder=10
    ;;
    7)
    	identifier=2
        best_finder=10
        builder=10
    ;;
    8)
    	identifier=3
        best_finder=10
        builder=10
    ;;
    *)
        echo "no case"
    ;;
esac

size=$2
try=$3

java -cp training.jar controller.Training --size=$size --parallel --identifier=$identifier --best-finder=$best_finder --builder=$builder > logs/eksperimen_$2_parallel_$1_$3.log 2>&1 &