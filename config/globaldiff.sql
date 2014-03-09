select * from
(	select * from (select * from states where id=0) as gl
	where not exists(
		select * from (select * from states where id=?) as cl 
		where 
			(gl.x = cl.x and gl.y = cl.y and gl.val = cl.val))
) as part1
UNION
(	select * from (select * from states where id=?) as cl2
	where not exists(
		select * from (select * from states where id=0) as gl2 
		where 
			(gl2.x = cl2.x and gl2.y = cl2.y))
);