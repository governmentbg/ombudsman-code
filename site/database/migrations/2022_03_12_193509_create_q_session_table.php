<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQSessionTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('q_session', function (Blueprint $table) {
            $table->increments('Qs_id');
            $table->integer('Qt_id')->comment('Topic ID')->unsigned()->nullable();
            $table->string('Qs_session', 90)->index();



            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Qt_id')->references('Qt_id')->on('q_topic');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('q_session');
    }
}
